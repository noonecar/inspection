package com.inspection.service.impl;

import com.inspection.common.enums.TaskStatus;
import com.inspection.common.exception.BusinessException;
import com.inspection.common.exception.EntityNotFoundException;
import com.inspection.common.exception.RegulationViolationException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.InspectionRecord;
import com.inspection.entity.InspectionTask;
import com.inspection.entity.RadioStation;
import com.inspection.entity.WarningRecord;
import com.inspection.mapper.InspectionRecordMapper;
import com.inspection.service.InspectionRecordService;
import com.inspection.service.InspectionTaskService;
import com.inspection.service.RadioStationService;
import com.inspection.service.WarningRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class InspectionRecordServiceImpl extends ServiceImpl<InspectionRecordMapper, InspectionRecord> implements InspectionRecordService {
    private static final Set<String> CHECK_TYPES = Set.of("书面检查", "现场核查", "监测", "检测", "提前核查", "监测及分析计算", "检测/监测", "书面检查/监测", "现场核查/监测/检测");
    private static final Set<String> INSPECTION_MODES = Set.of("日常检查", "重点检查");

    private final InspectionTaskService taskService;
    private final RadioStationService stationService;
    private final WarningRecordService warningRecordService;

    public InspectionRecordServiceImpl(InspectionTaskService taskService,
                                   RadioStationService stationService,
                                   WarningRecordService warningRecordService) {
        this.taskService = taskService;
        this.stationService = stationService;
        this.warningRecordService = warningRecordService;
    }

    @Override
    public InspectionRecord createRecord(InspectionRecord record) throws BusinessException {
        validateAndNormalize(record, null);
        determineRoundForCreate(record);
        save(record);
        afterRecordChanged(record);
        return record;
    }

    @Override
    public InspectionRecord updateRecord(Long id, InspectionRecord record) throws BusinessException {
        InspectionRecord existing = getById(id);
        if (existing == null) {
            throw new EntityNotFoundException("检查记录不存在，无法更新");
        }
        record.setId(id);
        record.setRound(existing.getRound());
        validateAndNormalize(record, id);
        updateById(record);
        afterRecordChanged(record);
        return record;
    }

    private void determineRoundForCreate(InspectionRecord record) {
        if (record.getRound() != null && record.getRound() > 0) {
            return;
        }
        InspectionRecord latest = lambdaQuery()
                .eq(InspectionRecord::getTaskId, record.getTaskId())
                .eq(InspectionRecord::getStationId, record.getStationId())
                .orderByDesc(InspectionRecord::getRound)
                .last("LIMIT 1")
                .one();
        if (latest != null) {
            record.setRound(latest.getRound() + 1);
        } else {
            record.setRound(1);
        }
    }

    @Override
    public void recalculateTaskStatus(Long taskId) {
        InspectionTask task = taskService.getById(taskId);
        if (task == null || TaskStatus.CANCELLED.getLabel().equals(task.getStatus())) {
            return;
        }

        List<Long> stationIds = getStationIdsForTask(task);
        if (stationIds.isEmpty()) {
            return;
        }

        boolean allQualified = true;
        boolean anyFailed = false;
        for (Long sid : stationIds) {
            InspectionRecord latestRecord = lambdaQuery()
                    .eq(InspectionRecord::getTaskId, taskId)
                    .eq(InspectionRecord::getStationId, sid)
                    .orderByDesc(InspectionRecord::getRound)
                    .last("LIMIT 1")
                    .one();
            if (latestRecord == null) {
                allQualified = false;
                continue;
            }
            if (!"合格".equals(latestRecord.getResult())) {
                allQualified = false;
                anyFailed = true;
            }
        }

        if (allQualified) {
            task.setStatus(TaskStatus.COMPLETED.getLabel());
            taskService.updateById(task);
            warningRecordService.resolveWarningsByTaskId(taskId);
        } else if (anyFailed) {
            task.setStatus(TaskStatus.PENDING_REINSPECTION.getLabel());
            taskService.updateById(task);
        }
    }

    private void afterRecordChanged(InspectionRecord record) {
        InspectionTask task = taskService.getById(record.getTaskId());
        if (task == null || TaskStatus.CANCELLED.getLabel().equals(task.getStatus())) {
            return;
        }

        List<Long> stationIds = getStationIdsForTask(task);
        if (stationIds.isEmpty()) {
            return;
        }

        boolean allQualified = true;
        boolean anyFailed = false;
        for (Long sid : stationIds) {
            InspectionRecord latestRecord = lambdaQuery()
                    .eq(InspectionRecord::getTaskId, record.getTaskId())
                    .eq(InspectionRecord::getStationId, sid)
                    .orderByDesc(InspectionRecord::getRound)
                    .last("LIMIT 1")
                    .one();
            if (latestRecord == null) {
                allQualified = false;
                continue;
            }
            if (!"合格".equals(latestRecord.getResult())) {
                allQualified = false;
                anyFailed = true;
            }
        }

        if (allQualified) {
            task.setStatus(TaskStatus.COMPLETED.getLabel());
            taskService.updateById(task);
            warningRecordService.resolveWarningsByTaskId(record.getTaskId());
        } else if (anyFailed) {
            task.setStatus(TaskStatus.PENDING_REINSPECTION.getLabel());
            taskService.updateById(task);
        }

        generateWarningIfNeeded(record);
    }

    private List<Long> getStationIdsForTask(InspectionTask task) {
        if (task.getStationIds() != null && !task.getStationIds().isBlank()) {
            try {
                List<Long> ids = com.alibaba.fastjson2.JSON.parseArray(task.getStationIds(), Long.class);
                if (ids != null && !ids.isEmpty()) {
                    return ids;
                }
            } catch (Exception ignored) {
                // fall through to use single stationId
            }
        }
        if (task.getStationId() != null) {
            List<Long> single = new ArrayList<>();
            single.add(task.getStationId());
            return single;
        }
        return List.of();
    }

    private void generateWarningIfNeeded(InspectionRecord record) {
        String level = null;
        String message = null;
        String ruleName = null;

        String taskName = "";
        String stationName = "";
        InspectionTask task = taskService.getById(record.getTaskId());
        if (task != null) {
            taskName = task.getName();
        }
        RadioStation station = stationService.getByStationId(record.getStationId());
        if (station != null) {
            stationName = station.getName();
        }
        String prefix = taskName != null && !taskName.isEmpty() ? taskName : ("任务" + record.getTaskId());
        String stationInfo = stationName != null && !stationName.isEmpty() ? ("台站[" + stationName + "]") : "";

        if (Boolean.TRUE.equals(record.getHarmfulInterference())) {
            level = "高";
            ruleName = "监测发现异常需重点检查";
            message = "[自动预警] " + prefix + " 检查" + stationInfo + "发现有害干扰，需立即重点处置。";
        } else if ("不合格".equals(record.getResult()) && Boolean.TRUE.equals(record.getNeedsRectification())) {
            level = "高";
            String detail = record.getWarningRuleDetail();
            if (detail != null && !detail.isBlank()) {
                ruleName = "违反无线电管理法规并被责令改正或行政处罚";
                message = "[自动预警] " + prefix + " 检查" + stationInfo + "不合格项：" + detail + "，请尽快复核并跟踪整改情况。";
            } else {
                ruleName = "违反无线电管理法规并被责令改正或行政处罚";
                message = "[自动预警] " + prefix + " 检查" + stationInfo + "结果不合格且需整改，请尽快复核并跟踪整改情况。";
            }
        } else if (Boolean.FALSE.equals(record.getAnnualReportSubmitted())) {
            level = "中";
            ruleName = "未按要求报送年度无线电频率使用报告或报告真实性存疑";
            message = "[自动预警] " + prefix + " 检查" + stationInfo + "发现年度频率使用报告未按要求报送，请督促限期补报。";
        } else if (Boolean.FALSE.equals(record.getSpectrumFeePaid())) {
            level = "中";
            ruleName = "频率占用费逾期未缴纳";
            message = "[自动预警] " + prefix + " 检查" + stationInfo + "发现频率占用费未缴纳，违反频率资源有偿使用规定，建议催缴并加收滞纳金。";
        } else if (Boolean.FALSE.equals(record.getTechnicalPersonnelOk())) {
            level = "中";
            ruleName = "未进行定期维护导致性能指标不符合规定";
            message = "[自动预警] " + prefix + " 检查" + stationInfo + "发现专业技术人员配置不符合要求，建议安排专项整改。";
        } else if (Boolean.TRUE.equals(record.getIncreasedFrequencyRequired())) {
            level = "低";
            ruleName = "监测发现异常需重点检查";
            message = "[自动预警] " + prefix + " 检查" + stationInfo + "建议关注频率增配或优化配置。";
        }

        if (ruleName != null) {
            WarningRecord warning = new WarningRecord();
            warning.setRuleName(ruleName);
            warning.setLevel(level == null ? "中" : level);
            warning.setMessage(message);
            warning.setStatus("未处理");
            warning.setRelatedTaskId(record.getTaskId());
            warningRecordService.upsertByRuleAndTask(warning);
        }
    }

    private void validateAndNormalize(InspectionRecord record, Long id) throws BusinessException {
        if (!CHECK_TYPES.contains(record.getCheckType())) {
            throw new RegulationViolationException("检查方式不在法定检查方式范围内");
        }
        if (!INSPECTION_MODES.contains(record.getInspectionMode())) {
            throw new RegulationViolationException("检查模式必须是日常检查或重点检查");
        }

        if (record.getCheckedAt() != null && record.getCheckedAt().isAfter(LocalDateTime.now())) {
            throw new RegulationViolationException("检查时间不能晚于当前时间");
        }

        InspectionTask task = taskService.getById(record.getTaskId());
        if (task == null) {
            throw new EntityNotFoundException("关联任务不存在");
        }
        if (task.getInspectionMode() != null && !task.getInspectionMode().equals(record.getInspectionMode())) {
            throw new RegulationViolationException("记录检查模式与任务检查模式不一致");
        }

        RadioStation station = stationService.getByStationId(record.getStationId());
        if (station == null) {
            throw new EntityNotFoundException("关联台站不存在");
        }
        if (record.getStationCategory() == null || record.getStationCategory().isBlank()) {
            record.setStationCategory(station.getStationClass());
        }

        if (record.getItemCode() != null && record.getItemCode().startsWith("1.5")) {
            String method = record.getCheckMethod() == null ? "" : record.getCheckMethod();
            if (!method.contains("监测") && !method.contains("分析计算")) {
                throw new RegulationViolationException("检查项1.5（使用率要求）必须采用监测及分析计算方法");
            }
        }

        if (Boolean.FALSE.equals(record.getAnnualReportSubmitted())) {
            record.setIncreasedFrequencyRequired(true);
        }

        if (Boolean.TRUE.equals(record.getNeedsRectification())) {
            if (!"不合格".equals(record.getResult())) {
                throw new RegulationViolationException("需整改记录必须判定为不合格");
            }
            if (record.getRectificationDeadline() == null || record.getRectificationDeadline().isBefore(LocalDate.now())) {
                throw new RegulationViolationException("需整改记录必须给出有效整改期限");
            }
        }

        if (Boolean.FALSE.equals(record.getSpectrumFeePaid())
                || Boolean.FALSE.equals(record.getTechnicalPersonnelOk())
                || Boolean.TRUE.equals(record.getHarmfulInterference())) {
            record.setResult("不合格");
            if (record.getNeedsRectification() == null) {
                record.setNeedsRectification(true);
            }
        }

        if (record.getIncreasedFrequencyRequired() == null) {
            record.setIncreasedFrequencyRequired(false);
        }
        if (record.getAnnualReportSubmitted() == null) {
            record.setAnnualReportSubmitted(true);
        }
        if (record.getSpectrumFeePaid() == null) {
            record.setSpectrumFeePaid(true);
        }
        if (record.getTechnicalPersonnelOk() == null) {
            record.setTechnicalPersonnelOk(true);
        }
        if (record.getHarmfulInterference() == null) {
            record.setHarmfulInterference(false);
        }
        if (record.getNeedsRectification() == null) {
            record.setNeedsRectification(false);
        }
        if (record.getProcessRecord() == null) {
            record.setProcessRecord("");
        }
        if (record.getEvidenceUrls() == null) {
            record.setEvidenceUrls("");
        }
        if (record.getAuditStatus() == null || record.getAuditStatus().isBlank()) {
            record.setAuditStatus("待审核");
        }
    }
}
