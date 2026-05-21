package com.inspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.enums.TaskStatus;
import com.inspection.common.annotation.OperationLog;
import com.inspection.common.exception.BusinessException;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.InspectionTask;
import com.inspection.entity.SysUser;
import com.inspection.entity.TaskHistory;
import com.inspection.entity.WarningRecord;
import com.inspection.service.InspectionTaskService;
import com.inspection.service.SysUserService;
import com.inspection.service.TaskHistoryService;
import com.inspection.service.WarningRecordService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final InspectionTaskService taskService;
    private final WarningRecordService warningService;
    private final SysUserService sysUserService;
    private final TaskHistoryService taskHistoryService;

    public TaskController(InspectionTaskService taskService,
                          WarningRecordService warningService,
                          SysUserService sysUserService,
                          TaskHistoryService taskHistoryService) {
        this.taskService = taskService;
        this.warningService = warningService;
        this.sysUserService = sysUserService;
        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<InspectionTask>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                                  @RequestParam(name = "size", defaultValue = "10") long size,
                                                  @RequestParam(name = "keyword", required = false) String keyword,
                                                  @RequestParam(name = "taskType", required = false) String taskType,
                                                  @RequestParam(name = "checkCategory", required = false) String checkCategory,
                                                  @RequestParam(name = "inspectionMode", required = false) String inspectionMode,
                                                  @RequestParam(name = "triggerSource", required = false) String triggerSource,
                                                  @RequestParam(name = "status", required = false) String status,
                                                  @RequestParam(name = "taskId", required = false) Long taskId,
                                                  @RequestParam(name = "stationId", required = false) Long stationId,
                                                  @RequestParam(name = "assignee", required = false) String assignee) {
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(InspectionTask::getName, keyword)
                    .or().like(InspectionTask::getAssignee, keyword)
                    .or().like(InspectionTask::getDescription, keyword));
        }
        if (inspectionMode != null && !inspectionMode.isBlank()) {
            wrapper.eq(InspectionTask::getInspectionMode, inspectionMode);
        }
        if (taskType != null && !taskType.isBlank()) {
            wrapper.eq(InspectionTask::getTaskType, taskType);
        }
        if (checkCategory != null && !checkCategory.isBlank()) {
            wrapper.eq(InspectionTask::getCheckCategory, checkCategory);
        }
        if (triggerSource != null && !triggerSource.isBlank()) {
            wrapper.eq(InspectionTask::getTriggerSource, triggerSource);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(InspectionTask::getStatus, status);
        }
        if (taskId != null) {
            wrapper.eq(InspectionTask::getId, taskId);
        }
        if (stationId != null) {
            wrapper.eq(InspectionTask::getStationId, stationId);
        }
        if (assignee != null && !assignee.isBlank()) {
            wrapper.like(InspectionTask::getAssignee, assignee);
        }

        if (isInspector()) {
            wrapper.eq(InspectionTask::getAssignee, currentUsername());
        }

        List<InspectionTask> list = taskService.list(wrapper.orderByDesc(InspectionTask::getId));
        Page<InspectionTask> result = new Page<>(page, size);
        result.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        result.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(result);
    }

    @GetMapping("/inspectors")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<Map<String, Object>>> listInspectors() {
        List<Map<String, Object>> data = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "INSPECTOR")
                .eq(SysUser::getEnabled, true)
                .orderByAsc(SysUser::getUsername)
                .list()
                .stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("username", item.getUsername());
                    map.put("realName", item.getRealName());
                    return map;
                })
                .toList();
        return ApiResponse.ok(data);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "任务统筹", operationType = "新增", description = "新增检查任务")
    public ApiResponse<InspectionTask> create(@Valid @RequestBody InspectionTask task) throws BusinessException {
        InspectionTask created = taskService.createTask(task);
        recordHistory(created.getId(), "新建", "新建检查任务");
        return ApiResponse.ok("创建成功", created);
    }

    @PutMapping("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "任务统筹", operationType = "修改", description = "修改检查任务")
    public ApiResponse<InspectionTask> update(@PathVariable("id") Long id, @Valid @RequestBody InspectionTask task) throws BusinessException {
        InspectionTask updated = taskService.updateTask(id, task);
        recordHistory(id, "编辑", "编辑检查任务");
        return ApiResponse.ok("更新成功", updated);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "任务统筹", operationType = "删除", description = "删除检查任务")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        recordHistory(id, "删除", "删除检查任务");
        taskService.removeById(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/batch-create")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> batchCreate(@RequestBody List<@Valid InspectionTask> tasks) throws BusinessException {
        if (tasks == null || tasks.isEmpty()) {
            return ApiResponse.ok("无数据", 0);
        }
        for (InspectionTask task : tasks) {
            taskService.createTask(task);
        }
        return ApiResponse.ok("批量创建成功", tasks.size());
    }

    @PostMapping("/batch-assign")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> batchAssign(@RequestBody Map<String, Object> request) {
        String assignee = request.get("assignee") == null ? null : request.get("assignee").toString();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty() || assignee == null || assignee.isBlank()) {
            return ApiResponse.ok("未执行分配", 0);
        }
        int updated = 0;
        for (Number id : ids) {
            InspectionTask task = taskService.getById(id.longValue());
            if (task == null) {
                continue;
            }
            task.setAssignee(assignee);
            taskService.updateById(task);
            recordHistory(id.longValue(), "分配检查员", "分配检查员：" + assignee);
            updated++;
        }
        return ApiResponse.ok("批量分配成功", updated);
    }

    @PostMapping("/audit")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "任务统筹", operationType = "审核", description = "审核任务")
    public ApiResponse<Integer> audit(@RequestBody Map<String, Object> request) {
        String result = request.get("result") == null ? null : request.get("result").toString();
        String reason = request.get("reason") == null ? "审核未通过" : request.get("reason").toString();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty() || result == null || result.isBlank()) {
            return ApiResponse.ok("未执行审核", 0);
        }
        boolean approved = "通过".equals(result);
        int updated = 0;
        for (Number id : ids) {
            InspectionTask task = taskService.getById(id.longValue());
            if (task == null) {
                continue;
            }
            if (!TaskStatus.PENDING_REVIEW.getLabel().equals(task.getStatus())) {
                continue;
            }
            if (approved) {
                task.setStatus(TaskStatus.IN_PROGRESS.getLabel());
                recordHistory(id.longValue(), "审核通过", "审核通过");
            } else {
                task.setStatus(TaskStatus.CANCELLED.getLabel());
                task.setCancelReason(reason);
                recordHistory(id.longValue(), "审核驳回", "审核驳回：" + reason);
            }
            taskService.updateById(task);
            updated++;
        }
        return ApiResponse.ok("审核完成", updated);
    }

    @PostMapping("/batch-status")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> batchStatus(@RequestBody Map<String, Object> request) {
        String status = request.get("status") == null ? null : request.get("status").toString();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty() || status == null || status.isBlank()) {
            return ApiResponse.ok("未执行更新", 0);
        }
        if (TaskStatus.fromLabel(status) == null) {
            return ApiResponse.fail("无效的任务状态: " + status);
        }
        int updated = 0;
        for (Number id : ids) {
            InspectionTask task = taskService.getById(id.longValue());
            if (task == null) {
                continue;
            }
            task.setStatus(status);
            taskService.updateById(task);
            recordHistory(id.longValue(), "状态更新", "状态更新为：" + status);
            updated++;
        }
        return ApiResponse.ok("批量更新成功", updated);
    }

    @PostMapping("/batch-remind")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> batchRemind(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok("未执行催办", 0);
        }
        int updated = 0;
        for (Number id : ids) {
            InspectionTask task = taskService.getById(id.longValue());
            if (task == null) {
                continue;
            }
            task.setRemindedAt(LocalDateTime.now());
            taskService.updateById(task);
            recordHistory(id.longValue(), "任务催办", "任务催办");

            if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
                WarningRecord reminderPopup = new WarningRecord();
                reminderPopup.setRuleName("任务催办弹窗");
                reminderPopup.setLevel("中");
                reminderPopup.setStatus("未处理");
                reminderPopup.setRelatedTaskId(task.getId());
                reminderPopup.setMessage("催办通知：任务【" + task.getName() + "】已催办，请检查员 " + task.getAssignee() + " 尽快处理。");
                warningService.upsertByRuleAndTask(reminderPopup);
            }
            updated++;
        }
        return ApiResponse.ok("批量催办成功", updated);
    }

    @PostMapping("/batch-cancel")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> batchCancel(@RequestBody Map<String, Object> request) {
        String cancelReason = request.get("cancelReason") == null ? "任务取消" : request.get("cancelReason").toString();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok("未执行撤销", 0);
        }
        int updated = 0;
        for (Number id : ids) {
            InspectionTask task = taskService.getById(id.longValue());
            if (task == null) {
                continue;
            }
            task.setStatus(TaskStatus.CANCELLED.getLabel());
            task.setCancelReason(cancelReason);
            taskService.updateById(task);
            recordHistory(id.longValue(), "任务撤销", "任务撤销：" + cancelReason);
            updated++;
        }
        return ApiResponse.ok("批量撤销成功", updated);
    }

    @GetMapping("/progress-summary")
        @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Map<String, Object>> progressSummary() {
        String username = currentUsername();
        boolean inspector = isInspector();
        long total = inspector
            ? taskService.lambdaQuery().eq(InspectionTask::getAssignee, username).count()
            : taskService.count();
        long ongoing = inspector
            ? taskService.lambdaQuery().eq(InspectionTask::getAssignee, username).eq(InspectionTask::getStatus, TaskStatus.IN_PROGRESS.getLabel()).count()
            : taskService.lambdaQuery().eq(InspectionTask::getStatus, TaskStatus.IN_PROGRESS.getLabel()).count();
        long reviewing = inspector
            ? taskService.lambdaQuery().eq(InspectionTask::getAssignee, username).eq(InspectionTask::getStatus, TaskStatus.PENDING_REVIEW.getLabel()).count()
            : taskService.lambdaQuery().eq(InspectionTask::getStatus, TaskStatus.PENDING_REVIEW.getLabel()).count();
        long completed = inspector
            ? taskService.lambdaQuery().eq(InspectionTask::getAssignee, username).eq(InspectionTask::getStatus, TaskStatus.COMPLETED.getLabel()).count()
            : taskService.lambdaQuery().eq(InspectionTask::getStatus, TaskStatus.COMPLETED.getLabel()).count();
        long canceled = inspector
            ? taskService.lambdaQuery().eq(InspectionTask::getAssignee, username).eq(InspectionTask::getStatus, TaskStatus.CANCELLED.getLabel()).count()
            : taskService.lambdaQuery().eq(InspectionTask::getStatus, TaskStatus.CANCELLED.getLabel()).count();
        long pendingReinspection = inspector
            ? taskService.lambdaQuery().eq(InspectionTask::getAssignee, username).eq(InspectionTask::getStatus, TaskStatus.PENDING_REINSPECTION.getLabel()).count()
            : taskService.lambdaQuery().eq(InspectionTask::getStatus, TaskStatus.PENDING_REINSPECTION.getLabel()).count();
        long remindedToday = inspector
            ? taskService.lambdaQuery().eq(InspectionTask::getAssignee, username)
                .ge(InspectionTask::getRemindedAt, LocalDateTime.now().toLocalDate().atStartOfDay()).count()
            : taskService.lambdaQuery().ge(InspectionTask::getRemindedAt, LocalDateTime.now().toLocalDate().atStartOfDay()).count();

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("ongoing", ongoing);
        data.put("reviewing", reviewing);
        data.put("completed", completed);
        data.put("canceled", canceled);
        data.put("pendingReinspection", pendingReinspection);
        data.put("remindedToday", remindedToday);
        return ApiResponse.ok(data);
    }

    @GetMapping("/export-progress")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public void exportProgress(HttpServletResponse response) throws IOException {
        List<InspectionTask> list;
        if (isInspector()) {
            list = taskService.lambdaQuery().eq(InspectionTask::getAssignee, currentUsername()).orderByDesc(InspectionTask::getId).list();
        } else {
            list = taskService.lambdaQuery().orderByDesc(InspectionTask::getId).list();
        }
        StringBuilder csv = new StringBuilder("任务ID,任务名称,任务类型,检查细分类,检查方式,状态,执行人,计划日期,催办时间,撤销原因\n");
        for (InspectionTask task : list) {
            csv.append(task.getId()).append(',')
                    .append(safeCsv(task.getName())).append(',')
                    .append(safeCsv(task.getTaskType())).append(',')
                    .append(safeCsv(task.getCheckCategory())).append(',')
                    .append(safeCsv(task.getInspectionMode())).append(',')
                    .append(safeCsv(task.getStatus())).append(',')
                    .append(safeCsv(task.getAssignee())).append(',')
                    .append(task.getDueDate() == null ? "" : task.getDueDate()).append(',')
                    .append(task.getRemindedAt() == null ? "" : task.getRemindedAt()).append(',')
                    .append(safeCsv(task.getCancelReason()))
                    .append("\n");
        }
        writeCsvResponse(response, "task-progress-report.csv", csv.toString());
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

    private void recordHistory(Long taskId, String actionType, String description) {
        TaskHistory history = new TaskHistory();
        history.setTaskId(taskId);
        history.setActionType(actionType);
        history.setDescription(description);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            history.setOperator(auth.getName());
            history.setOperatorRole(resolveRole(auth.getAuthorities()));
        }
        taskHistoryService.save(history);
    }

    private String resolveRole(java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
        if (authorities == null) {
            return "";
        }
        return authorities.stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("");
    }

    @GetMapping("/{taskId}/history")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<TaskHistory>> getHistory(@PathVariable("taskId") Long taskId) {
        List<TaskHistory> history = taskHistoryService.lambdaQuery()
                .eq(TaskHistory::getTaskId, taskId)
                .orderByDesc(TaskHistory::getCreatedAt)
                .list();
        return ApiResponse.ok(history);
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
