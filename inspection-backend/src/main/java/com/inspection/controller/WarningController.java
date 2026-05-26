package com.inspection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.InspectionTask;
import com.inspection.entity.WarningHistory;
import com.inspection.entity.WarningRecord;
import com.inspection.service.InspectionTaskService;
import com.inspection.service.WarningHistoryService;
import com.inspection.service.WarningRecordService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@RestController
@RequestMapping("/api/warnings")
public class WarningController {
    private static final DateTimeFormatter NOTIFY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final WarningRecordService warningService;
    private final InspectionTaskService taskService;
    private final WarningHistoryService warningHistoryService;

    public WarningController(WarningRecordService warningService, InspectionTaskService taskService,
                         WarningHistoryService warningHistoryService) {
        this.warningService = warningService;
        this.taskService = taskService;
        this.warningHistoryService = warningHistoryService;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<WarningRecord>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                                 @RequestParam(name = "size", defaultValue = "10") long size,
                                                 @RequestParam(name = "keyword", required = false) String keyword,
                                                 @RequestParam(name = "ruleName", required = false) String ruleName,
                                                 @RequestParam(name = "level", required = false) String level,
                                                 @RequestParam(name = "status", required = false) String status,
                                                 @RequestParam(name = "relatedTaskId", required = false) Long relatedTaskId) {
        LambdaQueryWrapper<WarningRecord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(WarningRecord::getRuleName, keyword)
                    .or().like(WarningRecord::getMessage, keyword));
        }
        if (ruleName != null && !ruleName.isBlank()) {
            wrapper.like(WarningRecord::getRuleName, ruleName);
        }
        if (level != null && !level.isBlank()) {
            wrapper.eq(WarningRecord::getLevel, level);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(WarningRecord::getStatus, status);
        }
        if (relatedTaskId != null) {
            wrapper.eq(WarningRecord::getRelatedTaskId, relatedTaskId);
        }

        if (isInspector()) {
            List<Long> taskIds = taskService.lambdaQuery()
                    .apply("FIND_IN_SET({0}, inspector) > 0", currentUsername())
                    .list()
                    .stream()
                    .map(InspectionTask::getId)
                    .toList();
            if (taskIds.isEmpty()) {
                Page<WarningRecord> empty = new Page<>(page, size);
                empty.setTotal(0);
                empty.setRecords(List.of());
                return ApiResponse.ok(empty);
            }
            wrapper.in(WarningRecord::getRelatedTaskId, taskIds);
        }

        List<WarningRecord> list = warningService.list(wrapper.orderByDesc(WarningRecord::getId));
        Page<WarningRecord> res = new Page<>(page, size);
        res.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        res.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(res);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<WarningRecord> create(@Valid @RequestBody WarningRecord record) {
        warningService.save(record);
        return ApiResponse.ok("创建成功", record);
    }

    @PutMapping("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<WarningRecord> update(@PathVariable("id") Long id, @Valid @RequestBody WarningRecord record) {
        record.setId(id);
        warningService.updateWarning(record);
        return ApiResponse.ok("更新成功", record);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        warningService.removeById(id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/resolve")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Void> resolveWarning(@PathVariable("id") Long id) {
        WarningRecord record = warningService.getById(id);
        if (record == null) {
            return ApiResponse.fail("预警记录不存在");
        }
        warningService.resolveWarning(id);
        return ApiResponse.ok("预警已标记为已处理", null);
    }

    @GetMapping("/history/{warningId}")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<WarningHistory>> getHistoryByWarning(@PathVariable("warningId") Long warningId) {
        List<WarningHistory> history = warningHistoryService.lambdaQuery()
                .eq(WarningHistory::getWarningId, warningId)
                .orderByDesc(WarningHistory::getCreatedAt)
                .list();
        return ApiResponse.ok(history);
    }

    @GetMapping("/notifications")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<Map<String, Object>>> notifications(@RequestParam(name = "limit", defaultValue = "8") int limit) {
        List<WarningRecord> records;
        if (isInspector()) {
            List<Long> taskIds = taskService.lambdaQuery()
                    .apply("FIND_IN_SET({0}, inspector) > 0", currentUsername())
                    .list()
                    .stream()
                    .map(InspectionTask::getId)
                    .toList();
            if (taskIds.isEmpty()) {
                return ApiResponse.ok(List.of());
            }
            records = warningService.lambdaQuery()
                    .in(WarningRecord::getRelatedTaskId, taskIds)
                    .ne(WarningRecord::getStatus, "已处理")
                    .orderByDesc(WarningRecord::getId)
                    .list();
        } else {
            records = warningService.lambdaQuery()
                    .ne(WarningRecord::getStatus, "已处理")
                    .orderByDesc(WarningRecord::getId)
                    .list();
        }

        List<Map<String, Object>> data = records.stream().limit(Math.max(1, limit)).map(item -> {
            Map<String, Object> res = new java.util.HashMap<>();
            res.put("id", item.getId());
            res.put("text", item.getMessage());
            res.put("level", item.getLevel());
            res.put("status", item.getStatus());
            res.put("time", item.getCreatedAt() == null ? "" : item.getCreatedAt().format(NOTIFY_TIME_FORMAT));
            String path = "任务催办弹窗".equals(item.getRuleName()) ? "/task" : "/warning";
            res.put("path", path);
            res.put("relatedTaskId", item.getRelatedTaskId());
            return res;
        }).toList();
        return ApiResponse.ok(data);
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
}
