package com.inspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.annotation.OperationLog;
import com.inspection.common.exception.BusinessException;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.InspectionRecord;
import com.inspection.entity.InspectionTask;
import com.inspection.entity.TaskHistory;
import com.inspection.service.InspectionRecordService;
import com.inspection.service.InspectionTaskService;
import com.inspection.service.OperationLogService;
import com.inspection.service.TaskHistoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspections")
public class InspectionRecordController {
    private final InspectionRecordService recordService;
    private final InspectionTaskService taskService;
    private final OperationLogService operationLogService;
    private final TaskHistoryService taskHistoryService;
    private final JdbcTemplate jdbcTemplate;

    public InspectionRecordController(InspectionRecordService recordService,
                                      InspectionTaskService taskService,
                                      OperationLogService operationLogService,
                                      TaskHistoryService taskHistoryService,
                                      JdbcTemplate jdbcTemplate) {
        this.recordService = recordService;
        this.taskService = taskService;
        this.operationLogService = operationLogService;
        this.taskHistoryService = taskHistoryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<InspectionRecord>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                                    @RequestParam(name = "size", defaultValue = "10") long size,
                                                    @RequestParam(name = "keyword", required = false) String keyword,
                                                    @RequestParam(name = "result", required = false) String result,
                                                    @RequestParam(name = "inspectionMode", required = false) String inspectionMode,
                                                    @RequestParam(name = "checkType", required = false) String checkType,
                                                    @RequestParam(name = "stationCategory", required = false) String stationCategory,
                                                    @RequestParam(name = "objectType", required = false) String objectType,
                                                    @RequestParam(name = "auditStatus", required = false) String auditStatus,
                                                    @RequestParam(name = "taskId", required = false) Long taskId,
                                                    @RequestParam(name = "taskType", required = false) String taskType,
                                                    @RequestParam(name = "checkCategory", required = false) String checkCategory,
                                                    @RequestParam(name = "stationId", required = false) Long stationId,
                                                    @RequestParam(name = "itemCode", required = false) String itemCode,
                                                    @RequestParam(name = "inspector", required = false) String inspector,
                                                    @RequestParam(name = "triggerSource", required = false) String triggerSource,
                                                    @RequestParam(name = "needsRectification", required = false) Boolean needsRectification,
                                                    @RequestParam(name = "annualReportSubmitted", required = false) Boolean annualReportSubmitted,
                                                    @RequestParam(name = "spectrumFeePaid", required = false) Boolean spectrumFeePaid,
                                                    @RequestParam(name = "technicalPersonnelOk", required = false) Boolean technicalPersonnelOk,
                                                    @RequestParam(name = "harmfulInterference", required = false) Boolean harmfulInterference) {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();

        if (taskType != null && !taskType.isBlank() || checkCategory != null && !checkCategory.isBlank()) {
            LambdaQueryWrapper<InspectionTask> taskWrapper = new LambdaQueryWrapper<>();
            if (taskType != null && !taskType.isBlank()) {
                taskWrapper.eq(InspectionTask::getTaskType, taskType);
            }
            if (checkCategory != null && !checkCategory.isBlank()) {
                taskWrapper.eq(InspectionTask::getCheckCategory, checkCategory);
            }
            List<Long> taskIds = taskService.list(taskWrapper).stream().map(InspectionTask::getId).toList();
            if (taskIds.isEmpty()) {
                Page<InspectionRecord> empty = new Page<>(page, size);
                empty.setTotal(0);
                empty.setRecords(List.of());
                return ApiResponse.ok(empty);
            }
            wrapper.in(InspectionRecord::getTaskId, taskIds);
        }

        if (isInspector()) {
            List<Long> assignedTaskIds = taskService.lambdaQuery()
                    .apply("FIND_IN_SET({0}, inspector) > 0", currentUsername())
                    .list()
                    .stream()
                    .map(InspectionTask::getId)
                    .toList();
            if (assignedTaskIds.isEmpty()) {
                Page<InspectionRecord> empty = new Page<>(page, size);
                empty.setTotal(0);
                empty.setRecords(List.of());
                return ApiResponse.ok(empty);
            }
            wrapper.in(InspectionRecord::getTaskId, assignedTaskIds);
        }

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(InspectionRecord::getItemName, keyword)
                    .or().like(InspectionRecord::getItemCode, keyword)
                    .or().like(InspectionRecord::getInspector, keyword)
                    .or().like(InspectionRecord::getObjectType, keyword));
        }
        if (result != null && !result.isBlank()) {
            wrapper.eq(InspectionRecord::getResult, result);
        }
        if (inspectionMode != null && !inspectionMode.isBlank()) {
            wrapper.eq(InspectionRecord::getInspectionMode, inspectionMode);
        }
        if (checkType != null && !checkType.isBlank()) {
            wrapper.eq(InspectionRecord::getCheckType, checkType);
        }
        if (stationCategory != null && !stationCategory.isBlank()) {
            wrapper.eq(InspectionRecord::getStationCategory, stationCategory);
        }
        if (objectType != null && !objectType.isBlank()) {
            wrapper.eq(InspectionRecord::getObjectType, objectType);
        }
        if (auditStatus != null && !auditStatus.isBlank()) {
            wrapper.eq(InspectionRecord::getAuditStatus, auditStatus);
        }
        if (taskId != null) {
            wrapper.eq(InspectionRecord::getTaskId, taskId);
        }
        if (stationId != null) {
            wrapper.eq(InspectionRecord::getStationId, stationId);
        }
        if (itemCode != null && !itemCode.isBlank()) {
            wrapper.like(InspectionRecord::getItemCode, itemCode);
        }
        if (inspector != null && !inspector.isBlank()) {
            wrapper.like(InspectionRecord::getInspector, inspector);
        }
        if (triggerSource != null && !triggerSource.isBlank()) {
            wrapper.eq(InspectionRecord::getTriggerSource, triggerSource);
        }
        if (needsRectification != null) {
            wrapper.eq(InspectionRecord::getNeedsRectification, needsRectification);
        }
        if (annualReportSubmitted != null) {
            wrapper.eq(InspectionRecord::getAnnualReportSubmitted, annualReportSubmitted);
        }
        if (spectrumFeePaid != null) {
            wrapper.eq(InspectionRecord::getSpectrumFeePaid, spectrumFeePaid);
        }
        if (technicalPersonnelOk != null) {
            wrapper.eq(InspectionRecord::getTechnicalPersonnelOk, technicalPersonnelOk);
        }
        if (harmfulInterference != null) {
            wrapper.eq(InspectionRecord::getHarmfulInterference, harmfulInterference);
        }
        List<InspectionRecord> list = recordService.list(wrapper.orderByDesc(InspectionRecord::getId));
        Page<InspectionRecord> res = new Page<>(page, size);
        res.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        res.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(res);
    }

    @GetMapping("/{id}/audit-history")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<Map<String, Object>>> auditHistory(@PathVariable("id") Long id) {
        InspectionRecord record = recordService.getById(id);
        if (record == null) {
            return ApiResponse.ok(List.of());
        }
        if (isInspector()) {
            InspectionTask task = taskService.getById(record.getTaskId());
            if (task == null || !taskService.isInspectorAssigned(task.getAssignee(), currentUsername())) {
                return ApiResponse.ok(List.of());
            }
        }

        List<Map<String, Object>> timeline = new ArrayList<>();
        List<com.inspection.entity.OperationLog> logs = operationLogService.lambdaQuery()
            .eq(com.inspection.entity.OperationLog::getModuleName, "检查执行")
            .eq(com.inspection.entity.OperationLog::getTargetId, id)
            .orderByAsc(com.inspection.entity.OperationLog::getOperationTime)
                .list();

        for (com.inspection.entity.OperationLog log : logs) {
            Map<String, Object> item = new HashMap<>();
            item.put("time", log.getOperationTime());
            item.put("action", log.getOperationType());
            item.put("operator", log.getOperator());
            item.put("detail", log.getDescription());
            timeline.add(item);
        }

        if (record.getProcessRecord() != null && !record.getProcessRecord().isBlank()) {
            String[] lines = record.getProcessRecord().split("\\n");
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("time", null);
                item.put("action", "过程记录");
                item.put("operator", record.getInspector());
                item.put("detail", line.trim());
                timeline.add(item);
            }
        }

        timeline.sort(Comparator.comparing(o -> (LocalDateTime) o.get("time"), Comparator.nullsLast(Comparator.naturalOrder())));
        return ApiResponse.ok(timeline);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    @OperationLog(module = "检查执行", operationType = "新增", description = "新增检查记录")
    public ApiResponse<InspectionRecord> create(@Valid @RequestBody InspectionRecord record) throws BusinessException {
        if (isInspector() && !isAssignedToCurrentInspector(record.getTaskId())) {
            throw new BusinessException("检查员只能提交分配给自己的任务检查记录");
        }
        InspectionRecord created = recordService.createRecord(record);
        recordTaskCompletionHistory(record.getTaskId(), "检查执行");
        return ApiResponse.ok("创建成功", created);
    }

    @PutMapping("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    @OperationLog(module = "检查执行", operationType = "修改", description = "修改检查记录")
    public ApiResponse<InspectionRecord> update(@PathVariable("id") Long id, @Valid @RequestBody InspectionRecord record) throws BusinessException {
        if (isInspector() && !isAssignedToCurrentInspector(record.getTaskId())) {
            throw new BusinessException("检查员只能修改自己任务的检查记录");
        }
        return ApiResponse.ok("更新成功", recordService.updateRecord(id, record));
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "检查执行", operationType = "删除", description = "删除检查记录")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        InspectionRecord record = recordService.getById(id);
        if (record == null) {
            return ApiResponse.fail("记录不存在");
        }
        InspectionTask task = taskService.getById(record.getTaskId());
        if (task != null) {
            String detailTable = resolveDetailTable(task.getTaskType(), task.getCheckCategory());
            if (detailTable != null) {
                jdbcTemplate.update(
                    "DELETE FROM " + detailTable + " WHERE task_id = ? AND station_id = ?",
                    record.getTaskId(), record.getStationId()
                );
            }
        }
        recordService.removeById(id);
        recordService.recalculateTaskStatus(record.getTaskId());
        return ApiResponse.ok(null);
    }

    private String resolveDetailTable(String taskType, String checkCategory) {
        if ("频率检查任务".equals(taskType)) {
            return switch (checkCategory) {
                case "地面频率使用" -> "ground_frequency_records";
                case "卫星频率使用" -> "satellite_frequency_records";
                case "卫星通信网频率" -> "satellite_network_records";
                default -> null;
            };
        }
        if ("台站检查任务".equals(taskType)) {
            return switch (checkCategory) {
                case "地面台站" -> "ground_station_records";
                case "空间电台" -> "space_station_records";
                case "卫星地球站" -> "satellite_earth_station_records";
                default -> null;
            };
        }
        return null;
    }

    @PostMapping("/batch")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Integer> batchCreate(@RequestBody List<@Valid InspectionRecord> items) throws BusinessException {
        if (items == null || items.isEmpty()) {
            return ApiResponse.ok("无数据", 0);
        }
        for (InspectionRecord item : items) {
            if (isInspector() && !isAssignedToCurrentInspector(item.getTaskId())) {
                throw new BusinessException("批量提交中包含非当前检查员任务");
            }
            recordService.createRecord(item);
            recordTaskCompletionHistory(item.getTaskId(), "检查执行");
        }
        return ApiResponse.ok("批量录入成功", items.size());
    }

    @PostMapping("/audit")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "检查执行", operationType = "审核", description = "审核检查记录")
    public ApiResponse<Integer> audit(@RequestBody Map<String, Object> request) {
        String auditStatus = request.get("auditStatus") == null ? "已审核" : request.get("auditStatus").toString();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok("未执行审核", 0);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication == null ? "anonymous" : authentication.getName();

        int updated = 0;
        for (Number id : ids) {
            InspectionRecord record = recordService.getById(id.longValue());
            if (record == null) {
                continue;
            }
            record.setAuditStatus(auditStatus);
            if (record.getProcessRecord() == null) {
                record.setProcessRecord("");
            }
            record.setProcessRecord(record.getProcessRecord() + "\n[" + LocalDateTime.now() + "] 审核状态更新为: " + auditStatus);
            recordService.updateById(record);

            com.inspection.entity.OperationLog trace = new com.inspection.entity.OperationLog();
            trace.setModuleName("检查执行");
            trace.setOperationType("审核");
            trace.setTargetId(record.getId());
            trace.setOperator(operator);
            trace.setOperatorRole(authentication == null ? "UNKNOWN" : "AUTHENTICATED");
            trace.setOperationTime(LocalDateTime.now());
            trace.setDescription("审核记录ID=" + record.getId() + "，结果=" + auditStatus);
            operationLogService.save(trace);

            updated++;
        }
        return ApiResponse.ok("批量审核成功", updated);
    }

    @GetMapping("/export")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public void export(HttpServletResponse response) throws IOException {
        List<InspectionRecord> list;
        if (isInspector()) {
            List<Long> assignedTaskIds = taskService.lambdaQuery()
                    .apply("FIND_IN_SET({0}, inspector) > 0", currentUsername())
                    .list()
                    .stream()
                    .map(InspectionTask::getId)
                    .toList();
            if (assignedTaskIds.isEmpty()) {
                list = List.of();
            } else {
                list = recordService.lambdaQuery().in(InspectionRecord::getTaskId, assignedTaskIds).orderByDesc(InspectionRecord::getId).list();
            }
        } else {
            list = recordService.lambdaQuery().orderByDesc(InspectionRecord::getId).list();
        }
        StringBuilder csv = new StringBuilder("记录ID,任务ID,台站ID,对象类型,项号,检查内容,检查方式,结果,需整改,审核状态,检查员,检查时间\n");
        for (InspectionRecord record : list) {
            csv.append(record.getId()).append(',')
                    .append(record.getTaskId()).append(',')
                    .append(record.getStationId()).append(',')
                    .append(safeCsv(record.getObjectType())).append(',')
                    .append(safeCsv(record.getItemCode())).append(',')
                    .append(safeCsv(record.getItemName())).append(',')
                    .append(safeCsv(record.getCheckType())).append(',')
                    .append(safeCsv(record.getResult())).append(',')
                    .append(Boolean.TRUE.equals(record.getNeedsRectification()) ? "是" : "否").append(',')
                    .append(safeCsv(record.getAuditStatus())).append(',')
                    .append(safeCsv(record.getInspector())).append(',')
                    .append(record.getCheckedAt() == null ? "" : record.getCheckedAt())
                    .append("\n");
        }
        writeCsvResponse(response, "inspection-records.csv", csv.toString());
    }

    private void writeCsvResponse(HttpServletResponse response, String filename, String content) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            writer.write(content);
            writer.flush();
        }
    }

    private String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }

    private boolean isInspector() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(item -> "ROLE_INSPECTOR".equals(item.getAuthority()));
    }

    private boolean isAssignedToCurrentInspector(Long taskId) {
        if (taskId == null) {
            return false;
        }
        InspectionTask task = taskService.getById(taskId);
        return task != null && taskService.isInspectorAssigned(task.getAssignee(), currentUsername());
    }

    private void recordTaskCompletionHistory(Long taskId, String description) {
        InspectionTask task = taskService.getById(taskId);
        String inspectorName = task != null && task.getAssignee() != null ? task.getAssignee() : currentUsername();
        TaskHistory history = new TaskHistory();
        history.setTaskId(taskId);
        history.setActionType("检查执行");
        history.setDescription(description + "，检查员：" + inspectorName);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            history.setOperator(auth.getName());
            history.setOperatorRole(auth.getAuthorities() == null ? "" :
                    auth.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).findFirst().orElse(""));
        }
        taskHistoryService.save(history);
    }
}
