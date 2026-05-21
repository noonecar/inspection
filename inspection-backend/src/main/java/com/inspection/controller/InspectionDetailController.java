package com.inspection.controller;

import com.inspection.common.enums.TaskStatus;
import com.inspection.common.exception.BusinessException;
import com.inspection.common.exception.EntityNotFoundException;
import com.inspection.common.exception.RegulationViolationException;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.InspectionRecord;
import com.inspection.entity.InspectionTask;
import com.inspection.entity.RadioStation;
import com.inspection.entity.TaskHistory;
import com.inspection.service.InspectionRecordService;
import com.inspection.service.InspectionTaskService;
import com.inspection.service.RadioStationService;
import com.inspection.service.TaskHistoryService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/inspection-details")
public class InspectionDetailController {
    private static final String LEGAL_BASIS = "无线电频率使用和在用无线电台（站）监督检查暂行办法";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, DetailMeta> DETAIL_META = buildDetailMeta();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final InspectionTaskService taskService;
    private final RadioStationService stationService;
    private final InspectionRecordService recordService;
    private final TaskHistoryService taskHistoryService;

    public InspectionDetailController(NamedParameterJdbcTemplate jdbcTemplate,
                                      InspectionTaskService taskService,
                                      RadioStationService stationService,
                                      InspectionRecordService recordService,
                                      TaskHistoryService taskHistoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.taskService = taskService;
        this.stationService = stationService;
        this.recordService = recordService;
        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping("/{category}")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Map<String, Object>> getDetail(@PathVariable("category") String category,
                                                      @RequestParam("taskId") Long taskId,
                                                      @RequestParam(value = "stationId", required = false) Long stationId) throws BusinessException {
        DetailMeta meta = requireMeta(category);
        if (taskId == null) {
            throw new RegulationViolationException("任务ID不能为空");
        }
        InspectionTask task = taskService.getById(taskId);
        if (task == null) {
            throw new EntityNotFoundException("任务不存在");
        }
        if (isInspector() && !currentUsername().equals(task.getAssignee())) {
            throw new BusinessException("检查员只能查看分配给自己的任务明细");
        }

        Long effectiveStationId = stationId != null ? stationId : task.getStationId();

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + meta.tableName + " WHERE task_id = :taskId AND station_id = :stationId LIMIT 1",
                Map.of("taskId", taskId, "stationId", effectiveStationId)
        );
        if (!rows.isEmpty()) {
            response.putAll(normalizeDetailRow(rows.get(0)));
        }

        InspectionRecord record = recordService.lambdaQuery()
                .eq(InspectionRecord::getTaskId, taskId)
                .eq(InspectionRecord::getStationId, effectiveStationId)
                .orderByDesc(InspectionRecord::getRound)
                .last("LIMIT 1")
                .one();
        if (record != null) {
            if (!response.containsKey("inspectorName")) {
                response.put("inspectorName", record.getInspector());
            }
            if (!response.containsKey("isQualified")) {
                response.put("isQualified", "合格".equals(record.getResult()));
            }
            response.put("checkedAt", record.getCheckedAt() == null ? null : DATE_TIME.format(record.getCheckedAt()));
            response.put("rectificationDeadline", record.getRectificationDeadline());
            response.put("processRecord", record.getProcessRecord());
            response.put("evidenceUrls", record.getEvidenceUrls());
            response.put("auditStatus", record.getAuditStatus());
        }

        response.put("taskId", taskId);
        response.put("stationId", effectiveStationId);
        return ApiResponse.ok(response);
    }

    @PostMapping("/{category}")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Map<String, Object>> upsertDetail(@PathVariable("category") String category,
                                                         @RequestBody Map<String, Object> payload) throws BusinessException {
        DetailMeta meta = requireMeta(category);
        Long taskId = toLong(payload.get("taskId"), payload.get("task_id"));
        if (taskId == null) {
            throw new RegulationViolationException("任务ID不能为空");
        }
        InspectionTask task = taskService.getById(taskId);
        if (task == null) {
            throw new EntityNotFoundException("任务不存在");
        }
        if (isInspector() && !currentUsername().equals(task.getAssignee())) {
            throw new BusinessException("检查员只能录入分配给自己的任务明细");
        }

        Long stationId = toLong(payload.get("stationId"), payload.get("station_id"));
        if (stationId == null) {
            stationId = task.getStationId();
        }

        String inspectorName = toStringValue(payload.get("inspectorName"), payload.get("inspector_name"));
        if (inspectorName == null || inspectorName.isBlank()) {
            throw new RegulationViolationException("检查员不能为空");
        }

        Boolean isQualified = toBoolean(payload.get("isQualified"), payload.get("is_qualified"));
        if (isQualified == null) {
            throw new RegulationViolationException("请填写检查结论");
        }

        LocalDateTime checkedAt = toLocalDateTime(payload.get("checkedAt"));
        if (checkedAt == null) {
            throw new RegulationViolationException("检查时间不能为空");
        }

        Map<String, Object> detailValues = buildDetailValues(meta, payload, taskId, inspectorName, stationId);
        upsertDetailRow(meta, detailValues, taskId, stationId);

        InspectionRecord record = buildSummaryRecord(meta, payload, task, inspectorName, isQualified, checkedAt, stationId);
        record.setWarningRuleDetail(buildWarningRuleDetail(meta, payload, isQualified));
        InspectionRecord existing = recordService.lambdaQuery()
                .eq(InspectionRecord::getTaskId, taskId)
                .eq(InspectionRecord::getStationId, stationId)
                .orderByDesc(InspectionRecord::getRound)
                .last("LIMIT 1")
                .one();
        boolean isReinspection = existing != null
                && TaskStatus.PENDING_REINSPECTION.getLabel().equals(task.getStatus());

        if (isReinspection && existing != null && "合格".equals(existing.getResult())) {
            throw new BusinessException(
                    "该台站（ID=" + stationId + "）上次检查已合格，无需复检。仅不合格的台站需要复检。");
        }

        if (existing != null && !isReinspection) {
            record.setId(existing.getId());
            record.setRound(existing.getRound());
            if (record.getAuditStatus() == null || record.getAuditStatus().isBlank()) {
                record.setAuditStatus(existing.getAuditStatus());
            }
            if (record.getProcessRecord() == null) {
                record.setProcessRecord(existing.getProcessRecord());
            }
            if (record.getEvidenceUrls() == null) {
                record.setEvidenceUrls(existing.getEvidenceUrls());
            }
            recordService.updateRecord(existing.getId(), record);
        } else {
            recordService.createRecord(record);
        }

        recordTaskHistory(taskId, inspectorName, isReinspection ? "复检" : "检查执行");

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("stationId", stationId);
        response.put("category", category);
        return ApiResponse.ok("保存成功", response);
    }

    private Map<String, Object> buildDetailValues(DetailMeta meta,
                                                  Map<String, Object> payload,
                                                  Long taskId,
                                                  String inspectorName,
                                                  Long stationId) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("station_id", stationId);
        for (String column : meta.columns) {
            Object value = null;
            if ("task_id".equals(column)) {
                value = taskId;
            } else if ("inspector_name".equals(column)) {
                value = inspectorName;
            } else if ("station_id".equals(column)) {
                value = stationId;
            } else if ("is_qualified".equals(column)) {
                value = toBoolean(payload.get("isQualified"), payload.get("is_qualified"));
            } else if ("no_qualified_reason".equals(column)) {
                value = toStringValue(payload.get("noQualifiedReason"), payload.get("no_qualified_reason"));
            } else {
                value = payload.get(column);
            }
            if (value != null) {
                values.put(column, value);
            }
        }
        return values;
    }

    private void upsertDetailRow(DetailMeta meta, Map<String, Object> values, Long taskId, Long stationId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM " + meta.tableName + " WHERE task_id = :taskId AND station_id = :stationId LIMIT 1",
                Map.of("taskId", taskId, "stationId", stationId)
        );
        if (rows.isEmpty()) {
            String columns = String.join(", ", values.keySet());
            String placeholders = values.keySet().stream().map(key -> ":" + key).reduce((a, b) -> a + ", " + b).orElse("");
            String sql = "INSERT INTO " + meta.tableName + " (" + columns + ") VALUES (" + placeholders + ")";
            jdbcTemplate.update(sql, new MapSqlParameterSource(values));
        } else {
            List<String> sets = new ArrayList<>();
            for (String key : values.keySet()) {
                if (!"task_id".equals(key) && !"station_id".equals(key)) {
                    sets.add(key + " = :" + key);
                }
            }
            if (sets.isEmpty()) {
                return;
            }
            String sql = "UPDATE " + meta.tableName + " SET " + String.join(", ", sets) + " WHERE task_id = :task_id AND station_id = :station_id";
            Map<String, Object> params = new HashMap<>(values);
            params.put("task_id", taskId);
            params.put("station_id", stationId);
            jdbcTemplate.update(sql, new MapSqlParameterSource(params));
        }
    }

    private InspectionRecord buildSummaryRecord(DetailMeta meta,
                                                Map<String, Object> payload,
                                                InspectionTask task,
                                                String inspectorName,
                                                Boolean isQualified,
                                                LocalDateTime checkedAt,
                                                Long stationId) throws BusinessException {
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(task.getId());
        record.setStationId(stationId);
        record.setObjectType(meta.objectType);
        record.setItemCode(meta.itemCode);
        record.setItemName(meta.itemName);
        record.setCheckType(meta.checkType);
        record.setCheckMethod(meta.checkMethod);
        record.setLegalBasis(LEGAL_BASIS);
        record.setInspectionMode(task.getInspectionMode());
        record.setTriggerSource(task.getTriggerSource());

        RadioStation station = stationService.getByStationId(stationId);
        if (station != null) {
            record.setStationCategory(station.getStationClass());
        }

        record.setResult(Boolean.TRUE.equals(isQualified) ? "合格" : "不合格");
        record.setNeedsRectification(!Boolean.TRUE.equals(isQualified));

        record.setRectificationDeadline(toLocalDate(payload.get("rectificationDeadline")));
        record.setAnnualReportSubmitted(toBoolean(payload.get("annualReportSubmitted"), null, true));
        record.setSpectrumFeePaid(toBoolean(payload.get("spectrumFeePaid"), null, true));
        record.setTechnicalPersonnelOk(toBoolean(payload.get("technicalPersonnelOk"), null, true));
        record.setHarmfulInterference(toBoolean(payload.get("harmfulInterference"), null, false));
        record.setIncreasedFrequencyRequired(toBoolean(payload.get("increasedFrequencyRequired"), null, false));
        record.setProcessRecord(toStringValue(payload.get("processRecord"), null));
        record.setEvidenceUrls(toStringValue(payload.get("evidenceUrls"), null));
        record.setAuditStatus(toStringValue(payload.get("auditStatus"), null));
        record.setRemarks(toStringValue(payload.get("remarks"), null));
        record.setInspector(inspectorName);
        record.setCheckedAt(checkedAt);
        return record;
    }

    private String buildWarningRuleDetail(DetailMeta meta, Map<String, Object> payload, Boolean isQualified) {
        if (Boolean.TRUE.equals(isQualified)) {
            return null;
        }
        Set<String> details = new LinkedHashSet<>();
        for (String column : meta.columns) {
            if (!column.endsWith("_result")) {
                continue;
            }
            Object resultValue = payload.get(column);
            if (resultValue == null) {
                continue;
            }
            String resultText = String.valueOf(resultValue).trim();
            if (!"不合格".equals(resultText)) {
                continue;
            }
            String baseKey = column.substring(0, column.length() - "_result".length());
            Object rawValue = payload.get(baseKey);
            String text = rawValue == null ? "" : String.valueOf(rawValue).trim();
            details.add(text.isEmpty() ? baseKey : text);
        }
        if (details.isEmpty()) {
            String reason = toStringValue(payload.get("noQualifiedReason"), payload.get("no_qualified_reason"));
            if (reason != null && !reason.isBlank()) {
                details.add(reason);
            }
        }
        if (details.isEmpty()) {
            return null;
        }
        return String.join("；", details);
    }

    private Map<String, Object> normalizeDetailRow(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if ("task_id".equalsIgnoreCase(key)) {
                result.put("taskId", value);
            } else if ("inspector_name".equalsIgnoreCase(key)) {
                result.put("inspectorName", value);
            } else if ("is_qualified".equalsIgnoreCase(key)) {
                result.put("isQualified", value instanceof Number ? ((Number) value).intValue() == 1 : value);
            } else if ("no_qualified_reason".equalsIgnoreCase(key)) {
                result.put("noQualifiedReason", value);
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private DetailMeta requireMeta(String category) throws BusinessException {
        DetailMeta meta = DETAIL_META.get(category);
        if (meta == null) {
            throw new RegulationViolationException("不支持的检查类别");
        }
        return meta;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }

    private void recordTaskHistory(Long taskId, String inspectorName, String actionLabel) {
        TaskHistory history = new TaskHistory();
        history.setTaskId(taskId);
        history.setActionType(actionLabel);
        history.setDescription(actionLabel + "完成，检查员：" + inspectorName);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            history.setOperator(auth.getName());
            history.setOperatorRole(auth.getAuthorities() == null ? "" :
                    auth.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).findFirst().orElse(""));
        }
        taskHistoryService.save(history);
    }

    private boolean isInspector() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(item -> "ROLE_INSPECTOR".equals(item.getAuthority()));
    }

    private Long toLong(Object primary, Object fallback) {
        Object value = primary != null ? primary : fallback;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (!text.isEmpty()) {
                return Long.parseLong(text);
            }
        }
        return null;
    }

    private Boolean toBoolean(Object primary, Object fallback) {
        Object value = primary != null ? primary : fallback;
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "是".equals(text) || "合格".equals(text);
    }

    private Boolean toBoolean(Object primary, Object fallback, boolean defaultValue) {
        Boolean value = toBoolean(primary, fallback);
        return value == null ? defaultValue : value;
    }

    private String toStringValue(Object primary, Object fallback) {
        Object value = primary != null ? primary : fallback;
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (!text.isEmpty()) {
                if (text.length() == 10) {
                    return LocalDate.parse(text).atStartOfDay();
                }
                String normalized = text.replace('T', ' ');
                return LocalDateTime.parse(normalized, DATE_TIME);
            }
        }
        return null;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (!text.isEmpty()) {
                return LocalDate.parse(text);
            }
        }
        return null;
    }

    private static Map<String, DetailMeta> buildDetailMeta() {
        Map<String, DetailMeta> map = new HashMap<>();
        map.put("ground-frequency", new DetailMeta(
                "ground_frequency_records",
                List.of(
                        "task_id",
                        "station_id",
                        "inspector_name",
                        "is_qualified",
                        "no_qualified_reason",
                        "check_1_1_1_1",
                    "check_1_1_1_1_result",
                        "check_1_1_1_2",
                    "check_1_1_1_2_result",
                        "check_1_1_1_3",
                    "check_1_1_1_3_result",
                        "check_1_1_1_4",
                    "check_1_1_1_4_result",
                        "check_1_1_1_5",
                    "check_1_1_1_5_result",
                        "check_1_1_1_6_1",
                    "check_1_1_1_6_1_result",
                        "check_1_1_1_6_2",
                    "check_1_1_1_6_2_result",
                        "check_1_1_1_6_3",
                    "check_1_1_1_6_3_result",
                        "check_1_1_2",
                    "check_1_1_2_result",
                        "check_1_1_3",
                    "check_1_1_3_result",
                        "check_1_1_4",
                    "check_1_1_4_result",
                        "check_1_1_5",
                    "check_1_1_5_result",
                        "check_1_1_6",
                    "check_1_1_6_result",
                        "check_1_1_7",
                    "check_1_1_7_result",
                        "check_1_1_8",
                    "check_1_1_8_result",
                        "check_1_1_9",
                        "check_1_1_9_result"
                ),
                "无线电频率",
                "GF-01",
                "地面频率使用检查表",
                "现场核查/监测/检测",
                "按附件表逐项核查"
        ));
        map.put("satellite-frequency", new DetailMeta(
                "satellite_frequency_records",
                List.of(
                        "task_id",
                        "station_id",
                        "inspector_name",
                        "is_qualified",
                        "no_qualified_reason",
                        "check_1_2_1_1",
                    "check_1_2_1_1_result",
                        "check_1_2_1_2",
                    "check_1_2_1_2_result",
                        "check_1_2_1_3",
                    "check_1_2_1_3_result",
                        "check_1_2_1_4",
                    "check_1_2_1_4_result",
                        "check_1_2_1_5",
                    "check_1_2_1_5_result",
                        "check_1_2_1_6_1",
                    "check_1_2_1_6_1_result",
                        "check_1_2_1_6_2",
                    "check_1_2_1_6_2_result",
                        "check_1_2_1_6_3",
                    "check_1_2_1_6_3_result",
                        "check_1_2_1_6_4",
                    "check_1_2_1_6_4_result",
                        "check_1_2_1_6_5",
                    "check_1_2_1_6_5_result",
                        "check_1_2_2",
                    "check_1_2_2_result",
                        "check_1_2_3",
                    "check_1_2_3_result",
                        "check_1_2_4",
                    "check_1_2_4_result",
                        "check_1_2_5",
                    "check_1_2_5_result",
                        "check_1_2_6",
                    "check_1_2_6_result",
                        "check_1_2_7",
                    "check_1_2_7_result",
                        "check_1_2_8",
                    "check_1_2_8_result",
                        "check_1_2_9",
                        "check_1_2_9_result"
                ),
                "无线电频率",
                "SF-01",
                "卫星频率使用检查表",
                "现场核查/监测/检测",
                "按附件表逐项核查"
        ));
        map.put("satellite-network", new DetailMeta(
                "satellite_network_records",
                List.of(
                        "task_id",
                        "station_id",
                        "inspector_name",
                        "is_qualified",
                        "no_qualified_reason",
                        "check_1_3_1_1",
                    "check_1_3_1_1_result",
                        "check_1_3_1_2",
                    "check_1_3_1_2_result",
                        "check_1_3_1_3",
                    "check_1_3_1_3_result",
                        "check_1_3_1_4",
                    "check_1_3_1_4_result",
                        "check_1_3_1_5",
                    "check_1_3_1_5_result",
                        "check_1_3_1_6_1",
                    "check_1_3_1_6_1_result",
                        "check_1_3_1_6_2",
                    "check_1_3_1_6_2_result",
                        "check_1_3_1_6_3",
                    "check_1_3_1_6_3_result",
                        "check_1_3_1_6_4",
                    "check_1_3_1_6_4_result",
                        "check_1_3_1_6_5",
                    "check_1_3_1_6_5_result",
                        "check_1_3_2",
                    "check_1_3_2_result",
                        "check_1_3_3",
                    "check_1_3_3_result",
                        "check_1_3_4",
                    "check_1_3_4_result",
                        "check_1_3_5",
                    "check_1_3_5_result",
                        "check_1_3_6",
                    "check_1_3_6_result",
                        "check_1_3_7",
                    "check_1_3_7_result",
                        "check_1_3_8",
                    "check_1_3_8_result",
                        "check_1_3_9",
                        "check_1_3_9_result"
                ),
                "无线电频率",
                "SN-01",
                "卫星通信网频率检查表",
                "现场核查/监测/检测",
                "按附件表逐项核查"
        ));
        map.put("ground-station", new DetailMeta(
                "ground_station_records",
                List.of(
                        "task_id",
                        "station_id",
                        "inspector_name",
                        "is_qualified",
                        "no_qualified_reason",
                        "check_2_1_1_1",
                    "check_2_1_1_1_result",
                        "check_2_1_1_2_1",
                    "check_2_1_1_2_1_result",
                        "check_2_1_1_2_2",
                    "check_2_1_1_2_2_result",
                        "check_2_1_1_2_3",
                    "check_2_1_1_2_3_result",
                        "check_2_1_1_3_1",
                    "check_2_1_1_3_1_result",
                        "check_2_1_1_3_2",
                    "check_2_1_1_3_2_result",
                        "check_2_1_1_3_3",
                    "check_2_1_1_3_3_result",
                        "check_2_1_1_3_4",
                    "check_2_1_1_3_4_result",
                        "check_2_1_1_4_1",
                    "check_2_1_1_4_1_result",
                        "check_2_1_1_4_2",
                    "check_2_1_1_4_2_result",
                        "check_2_1_1_4_3",
                    "check_2_1_1_4_3_result",
                        "check_2_1_1_4_4",
                    "check_2_1_1_4_4_result",
                        "check_2_1_1_5_1",
                    "check_2_1_1_5_1_result",
                        "check_2_1_1_5_2",
                    "check_2_1_1_5_2_result",
                        "check_2_1_1_5_3",
                    "check_2_1_1_5_3_result",
                        "check_2_1_1_5_4",
                    "check_2_1_1_5_4_result",
                        "check_2_1_1_5_5",
                    "check_2_1_1_5_5_result",
                        "check_2_1_2",
                    "check_2_1_2_result",
                        "check_2_1_3",
                    "check_2_1_3_result",
                        "check_2_1_4",
                    "check_2_1_4_result",
                        "check_2_1_5",
                    "check_2_1_5_result",
                        "check_2_1_6",
                    "check_2_1_6_result",
                        "check_2_1_7",
                    "check_2_1_7_result",
                        "check_2_1_8",
                    "check_2_1_8_result",
                        "check_2_1_9",
                    "check_2_1_9_result",
                        "check_2_1_10",
                        "check_2_1_10_result"
                ),
                "在用无线电台（站）",
                "GS-01",
                "地面台站检查表",
                "现场核查/监测/检测",
                "按附件表逐项核查"
        ));
        map.put("space-station", new DetailMeta(
                "space_station_records",
                List.of(
                        "task_id",
                        "station_id",
                        "inspector_name",
                        "is_qualified",
                        "no_qualified_reason",
                        "check_2_2_1_1",
                    "check_2_2_1_1_result",
                        "check_2_2_1_2_1",
                    "check_2_2_1_2_1_result",
                        "check_2_2_1_2_2",
                    "check_2_2_1_2_2_result",
                        "check_2_2_1_2_3",
                    "check_2_2_1_2_3_result",
                        "check_2_2_1_3_1",
                    "check_2_2_1_3_1_result",
                        "check_2_2_1_3_2",
                    "check_2_2_1_3_2_result",
                        "check_2_2_1_3_3",
                    "check_2_2_1_3_3_result",
                        "check_2_2_1_3_4",
                    "check_2_2_1_3_4_result",
                        "check_2_2_1_4_1",
                    "check_2_2_1_4_1_result",
                        "check_2_2_1_4_2",
                    "check_2_2_1_4_2_result",
                        "check_2_2_1_4_3",
                    "check_2_2_1_4_3_result",
                        "check_2_2_1_4_4",
                    "check_2_2_1_4_4_result",
                        "check_2_2_2",
                    "check_2_2_2_result",
                        "check_2_2_3",
                    "check_2_2_3_result",
                        "check_2_2_4",
                    "check_2_2_4_result",
                        "check_2_2_5",
                    "check_2_2_5_result",
                        "check_2_2_6",
                    "check_2_2_6_result",
                        "check_2_2_7",
                        "check_2_2_7_result"
                ),
                "在用无线电台（站）",
                "SS-01",
                "空间电台检查表",
                "现场核查/监测/检测",
                "按附件表逐项核查"
        ));
        map.put("satellite-earth-station", new DetailMeta(
                "satellite_earth_station_records",
                List.of(
                        "task_id",
                        "station_id",
                        "inspector_name",
                        "is_qualified",
                        "no_qualified_reason",
                        "check_2_3_1_1",
                    "check_2_3_1_1_result",
                        "check_2_3_1_2_1",
                    "check_2_3_1_2_1_result",
                        "check_2_3_1_2_2",
                    "check_2_3_1_2_2_result",
                        "check_2_3_1_2_3",
                    "check_2_3_1_2_3_result",
                        "check_2_3_1_2_4",
                    "check_2_3_1_2_4_result",
                        "check_2_3_1_2_5",
                    "check_2_3_1_2_5_result",
                        "check_2_3_1_2_6",
                    "check_2_3_1_2_6_result",
                        "check_2_3_1_2_7",
                    "check_2_3_1_2_7_result",
                        "check_2_3_1_2_8",
                    "check_2_3_1_2_8_result",
                        "check_2_3_1_3_1",
                    "check_2_3_1_3_1_result",
                        "check_2_3_1_3_2",
                    "check_2_3_1_3_2_result",
                        "check_2_3_1_3_3",
                    "check_2_3_1_3_3_result",
                        "check_2_3_1_3_4",
                    "check_2_3_1_3_4_result",
                        "check_2_3_1_3_5",
                    "check_2_3_1_3_5_result",
                        "check_2_3_1_4_1",
                    "check_2_3_1_4_1_result",
                        "check_2_3_1_4_2",
                    "check_2_3_1_4_2_result",
                        "check_2_3_1_4_3",
                    "check_2_3_1_4_3_result",
                        "check_2_3_1_4_4",
                    "check_2_3_1_4_4_result",
                        "check_2_3_1_4_5",
                    "check_2_3_1_4_5_result",
                        "check_2_3_2",
                    "check_2_3_2_result",
                        "check_2_3_3",
                    "check_2_3_3_result",
                        "check_2_3_4",
                    "check_2_3_4_result",
                        "check_2_3_5",
                    "check_2_3_5_result",
                        "check_2_3_6",
                    "check_2_3_6_result",
                        "check_2_3_7",
                    "check_2_3_7_result",
                        "check_2_3_8",
                        "check_2_3_8_result"
                ),
                "在用无线电台（站）",
                "SE-01",
                "卫星地球站检查表",
                "现场核查/监测/检测",
                "按附件表逐项核查"
        ));
        return map;
    }

    private static class DetailMeta {
        private final String tableName;
        private final List<String> columns;
        private final String objectType;
        private final String itemCode;
        private final String itemName;
        private final String checkType;
        private final String checkMethod;

        private DetailMeta(String tableName,
                           List<String> columns,
                           String objectType,
                           String itemCode,
                           String itemName,
                           String checkType,
                           String checkMethod) {
            this.tableName = tableName;
            this.columns = columns;
            this.objectType = objectType;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.checkType = checkType;
            this.checkMethod = checkMethod;
        }
    }
}
