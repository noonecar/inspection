package com.inspection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.InspectionRecord;
import com.inspection.entity.InspectionTask;
import com.inspection.entity.RadioStation;
import com.inspection.entity.StatisticsReport;
import com.inspection.entity.WarningRecord;
import com.inspection.service.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final StatisticsReportService reportService;
    private final InspectionTaskService taskService;
    private final InspectionRecordService recordService;
    private final WarningRecordService warningService;
    private final RadioStationService stationService;

    public ReportController(StatisticsReportService reportService,
                            InspectionTaskService taskService,
                            InspectionRecordService recordService,
                            WarningRecordService warningService,
                            RadioStationService stationService) {
        this.reportService = reportService;
        this.taskService = taskService;
        this.recordService = recordService;
        this.warningService = warningService;
        this.stationService = stationService;
    }

    @GetMapping("/overview")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        if (isInspector()) {
            List<Long> taskIds = taskService.lambdaQuery().eq(InspectionTask::getAssignee, currentUsername()).list().stream().map(InspectionTask::getId).toList();
            if (taskIds.isEmpty()) {
                data.put("taskCount", 0);
                data.put("recordCount", 0);
                data.put("warningCount", 0);
            } else {
                data.put("taskCount", taskIds.size());
                data.put("recordCount", recordService.lambdaQuery().in(InspectionRecord::getTaskId, taskIds).count());
                data.put("warningCount", warningService.lambdaQuery().in(WarningRecord::getRelatedTaskId, taskIds).count());
            }
        } else {
            data.put("taskCount", taskService.count());
            data.put("recordCount", recordService.count());
            data.put("warningCount", warningService.count());
        }
        data.put("stationCount", stationService.count());
        data.put("reportCount", reportService.count());
        return ApiResponse.ok(data);
    }

    @GetMapping("/stats")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Map<String, Object>> stats(@RequestParam(name = "region", required = false) String region,
                                                  @RequestParam(name = "inspectionMode", required = false) String inspectionMode,
                                                  @RequestParam(name = "startDate", required = false) String startDate,
                                                  @RequestParam(name = "endDate", required = false) String endDate,
                                                  @RequestParam(name = "violationLevel", required = false) String violationLevel) {
        return ApiResponse.ok(buildStats(region, inspectionMode, startDate, endDate, violationLevel));
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<StatisticsReport>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                                    @RequestParam(name = "size", defaultValue = "10") long size) {
        List<StatisticsReport> list = reportService.list();
        Page<StatisticsReport> res = new Page<>(page, size);
        res.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        res.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(res);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<StatisticsReport> create(@RequestBody StatisticsReport report) {
        if (report.getGeneratedAt() == null) {
            report.setGeneratedAt(LocalDateTime.now());
        }
        reportService.save(report);
        return ApiResponse.ok("创建成功", report);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        reportService.removeById(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<StatisticsReport> detail(@PathVariable("id") Long id) {
        StatisticsReport report = reportService.getById(id);
        return ApiResponse.ok(report);
    }

    @GetMapping("/export")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public void export(@RequestParam(name = "format", defaultValue = "csv") String format,
                       @RequestParam(name = "region", required = false) String region,
                       @RequestParam(name = "inspectionMode", required = false) String inspectionMode,
                       @RequestParam(name = "startDate", required = false) String startDate,
                       @RequestParam(name = "endDate", required = false) String endDate,
                       @RequestParam(name = "violationLevel", required = false) String violationLevel,
                       HttpServletResponse response) throws IOException {
        Map<String, Object> stats = buildStats(region, inspectionMode, startDate, endDate, violationLevel);

        if ("pdf".equalsIgnoreCase(format)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"PDF导出已迁移为前端截图生成，请在页面点击导出PDF\"}");
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("统计报表模板导出\n");
        csv.append("导出时间,").append(LocalDateTime.now()).append("\n\n");

        csv.append("一、筛选条件\n");
        csv.append("区域,").append(safeCsv(defaultValue(region))).append("\n");
        csv.append("检查类型,").append(safeCsv(defaultValue(inspectionMode))).append("\n");
        csv.append("违规等级,").append(safeCsv(defaultValue(violationLevel))).append("\n");
        csv.append("开始日期,").append(safeCsv(defaultValue(startDate))).append("\n");
        csv.append("结束日期,").append(safeCsv(defaultValue(endDate))).append("\n\n");

        csv.append("二、统计概览\n");
        csv.append("检查记录总量,").append(stats.getOrDefault("recordTotal", 0)).append("\n");
        csv.append("预警总量,").append(stats.getOrDefault("warningTotal", 0)).append("\n\n");

        appendDistributionCsv(csv, "三、检查结果分布", castMap(stats.get("resultDistribution")));
        appendDistributionCsv(csv, "四、检查类型分布", castMap(stats.get("typeDistribution")));
        appendDistributionCsv(csv, "五、台站分类分布", castMap(stats.get("categoryDistribution")));
        appendDistributionCsv(csv, "六、违规等级分布", castMap(stats.get("violationDistribution")));
        writeCsvResponse(response, "statistics-report.csv", csv.toString());
    }

    private Map<String, Object> buildStats(String region,
                                           String inspectionMode,
                                           String startDate,
                                           String endDate,
                                           String violationLevel) {
        LocalDate start = startDate == null || startDate.isBlank() ? LocalDate.now().minusMonths(6) : LocalDate.parse(startDate);
        LocalDate end = endDate == null || endDate.isBlank() ? LocalDate.now() : LocalDate.parse(endDate);

        Map<Long, InspectionTask> taskMap = taskService.list().stream()
            .collect(Collectors.toMap(InspectionTask::getId, item -> item));

        List<InspectionRecord> filteredRecords = recordService.list().stream()
                .filter(item -> item.getCheckedAt() != null)
                .filter(item -> !item.getCheckedAt().toLocalDate().isBefore(start) && !item.getCheckedAt().toLocalDate().isAfter(end))
                .filter(item -> inspectionMode == null || inspectionMode.isBlank() || inspectionMode.equals(item.getInspectionMode()))
                .filter(item -> {
                    if (!isInspector()) {
                        return true;
                    }
                    InspectionTask task = taskMap.get(item.getTaskId());
                    return task != null && currentUsername().equals(task.getAssignee());
                })
                .toList();

        Map<Long, RadioStation> stationMap = stationService.list().stream()
                .collect(Collectors.toMap(RadioStation::getStationId, item -> item));
        if (region != null && !region.isBlank()) {
            filteredRecords = filteredRecords.stream()
                    .filter(item -> {
                        RadioStation station = stationMap.get(item.getStationId());
                        return station != null && station.getAddress() != null && station.getAddress().contains(region);
                    })
                    .toList();
        }

        // Task IDs for record-based distributions (filtered by date/mode/region)
        Set<Long> filteredTaskIds = filteredRecords.stream()
                .map(InspectionRecord::getTaskId)
                .collect(Collectors.toCollection(HashSet::new));

        // Task IDs for task-status and warning stats (region-filtered only, not limited by date/mode)
        Set<Long> regionFilteredTaskIds;
        if (region != null && !region.isBlank()) {
            regionFilteredTaskIds = taskMap.values().stream()
                .filter(task -> !isInspector() || currentUsername().equals(task.getAssignee()))
                .filter(task -> isTaskInRegion(task, stationMap, region))
                .map(InspectionTask::getId)
                .collect(Collectors.toCollection(HashSet::new));
        } else {
            regionFilteredTaskIds = taskMap.values().stream()
                .filter(task -> !isInspector() || currentUsername().equals(task.getAssignee()))
                .map(InspectionTask::getId)
                .collect(Collectors.toCollection(HashSet::new));
        }

        Map<String, Long> resultDistribution = filteredRecords.stream()
                .collect(Collectors.groupingBy(item -> defaultValue(item.getResult()), Collectors.counting()));
        Map<String, Long> typeDistribution = filteredRecords.stream()
                .collect(Collectors.groupingBy(item -> defaultValue(item.getInspectionMode()), Collectors.counting()));
        Map<String, Long> categoryDistribution = filteredRecords.stream()
                .collect(Collectors.groupingBy(item -> defaultValue(item.getStationCategory()), Collectors.counting()));

        Map<String, Long> taskStatusDistribution = taskMap.values().stream()
            .filter(item -> regionFilteredTaskIds.contains(item.getId()))
            .collect(Collectors.groupingBy(item -> defaultValue(item.getStatus()), Collectors.counting()));
        Map<String, Long> checkCategoryDistribution = filteredRecords.stream()
            .map(item -> taskMap.get(item.getTaskId()))
            .filter(item -> item != null)
            .collect(Collectors.groupingBy(item -> defaultValue(item.getCheckCategory()), Collectors.counting()));
        Map<String, Long> taskTypeDistribution = filteredRecords.stream()
            .map(item -> taskMap.get(item.getTaskId()))
            .filter(item -> item != null)
            .collect(Collectors.groupingBy(item -> defaultValue(item.getTaskType()), Collectors.counting()));

        List<WarningRecord> warnings = warningService.list().stream()
                .filter(item -> regionFilteredTaskIds.contains(item.getRelatedTaskId()))
                .filter(item -> violationLevel == null || violationLevel.isBlank() || violationLevel.equals(item.getLevel()))
                .toList();
        Map<String, Long> violationDistribution = warnings.stream()
                .collect(Collectors.groupingBy(item -> defaultValue(item.getStatus()) + "_" + defaultValue(item.getLevel()), Collectors.counting()));

        Map<String, Object> data = new HashMap<>();
        data.put("resultDistribution", resultDistribution);
        data.put("typeDistribution", typeDistribution);
        data.put("categoryDistribution", categoryDistribution);
        data.put("violationDistribution", violationDistribution);
        data.put("taskStatusDistribution", taskStatusDistribution);
        data.put("checkCategoryDistribution", checkCategoryDistribution);
        data.put("taskTypeDistribution", taskTypeDistribution);
        data.put("recordTotal", filteredRecords.size());
        data.put("warningTotal", warnings.size());
        return data;
    }

    private void appendDistributionCsv(StringBuilder csv, String title, Map<String, Long> distribution) {
        csv.append(title).append("\n");
        csv.append("分类,数量,图表信息\n");
        List<Map.Entry<String, Long>> list = sortDistribution(distribution);
        long max = list.stream().mapToLong(Map.Entry::getValue).max().orElse(1L);
        for (Map.Entry<String, Long> entry : list) {
            csv.append(safeCsv(entry.getKey())).append(',').append(entry.getValue()).append(',')
                    .append(safeCsv(buildBar(entry.getValue(), max))).append('\n');
        }
        if (list.isEmpty()) {
            csv.append("无数据,0,\n");
        }
        csv.append('\n');
    }

    private Map<String, Long> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Long> cast = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "未分类" : String.valueOf(entry.getKey());
                Long count = 0L;
                if (entry.getValue() instanceof Number n) {
                    count = n.longValue();
                }
                cast.put(key, count);
            }
            return cast;
        }
        return new HashMap<>();
    }

    private List<Map.Entry<String, Long>> sortDistribution(Map<String, Long> distribution) {
        return distribution.entrySet().stream()
                .sorted((a, b) -> {
                    int delta = Long.compare(b.getValue(), a.getValue());
                    if (delta != 0) {
                        return delta;
                    }
                    return a.getKey().compareTo(b.getKey());
                })
                .toList();
    }

    private String buildBar(long value, long max) {
        if (max <= 0) {
            return "";
        }
        int width = (int) Math.max(1, Math.round((value * 20.0) / max));
        return "█".repeat(width);
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "全部" : value;
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

    private boolean isTaskInRegion(InspectionTask task, Map<Long, RadioStation> stationMap, String region) {
        if (task.getStationIds() != null && !task.getStationIds().isBlank()) {
            try {
                List<Long> ids = com.alibaba.fastjson2.JSON.parseArray(task.getStationIds(), Long.class);
                if (ids != null && ids.stream().anyMatch(sid -> {
                    RadioStation station = stationMap.get(sid);
                    return station != null && station.getAddress() != null && station.getAddress().contains(region);
                })) return true;
            } catch (Exception ignored) {}
        }
        if (task.getStationId() != null) {
            RadioStation station = stationMap.get(task.getStationId());
            return station != null && station.getAddress() != null && station.getAddress().contains(region);
        }
        return false;
    }

    private boolean isInspector() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(item -> "ROLE_INSPECTOR".equals(item.getAuthority()));
    }
}
