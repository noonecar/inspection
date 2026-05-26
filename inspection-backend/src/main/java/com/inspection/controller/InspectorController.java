package com.inspection.controller;

import com.inspection.common.annotation.OperationLog;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.Institution;
import com.inspection.entity.InspectionRecord;
import com.inspection.entity.InspectionTask;
import com.inspection.entity.SysUser;
import com.inspection.entity.UserInstitution;
import com.inspection.service.InspectionRecordService;
import com.inspection.service.InspectionTaskService;
import com.inspection.service.InstitutionService;
import com.inspection.service.SysUserService;
import com.inspection.service.UserInstitutionService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/inspectors")
public class InspectorController {
    private final SysUserService sysUserService;
    private final InstitutionService institutionService;
    private final UserInstitutionService userInstitutionService;
    private final InspectionTaskService taskService;
    private final InspectionRecordService recordService;

    public InspectorController(SysUserService sysUserService,
                               InstitutionService institutionService,
                               UserInstitutionService userInstitutionService,
                               InspectionTaskService taskService,
                               InspectionRecordService recordService) {
        this.sysUserService = sysUserService;
        this.institutionService = institutionService;
        this.userInstitutionService = userInstitutionService;
        this.taskService = taskService;
        this.recordService = recordService;
    }

    @PostMapping
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "人员管理", operationType = "新增", description = "新增检查员账户")
    public ApiResponse<Map<String, Object>> createInspector(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String realName = (String) request.get("realName");
        Number institutionIdNum = (Number) request.get("institutionId");

        if (username == null || username.isBlank() || realName == null || realName.isBlank() || institutionIdNum == null) {
            return ApiResponse.fail("参数不完整");
        }

        long institutionId = institutionIdNum.longValue();
        Institution inst = institutionService.getById(institutionId);
        if (inst == null) {
            return ApiResponse.fail("机构不存在");
        }

        // Check duplicate username
        long exists = sysUserService.lambdaQuery().eq(SysUser::getUsername, username).count();
        if (exists > 0) {
            return ApiResponse.fail("账号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword("{noop}123456");
        user.setRealName(realName);
        user.setRole("INSPECTOR");
        user.setEnabled(true);
        sysUserService.save(user);

        UserInstitution ui = new UserInstitution();
        ui.setUserId(user.getId());
        ui.setInstitutionId(institutionId);
        userInstitutionService.save(ui);

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        return ApiResponse.ok("创建成功", data);
    }

    @PostMapping("/{id}/reset-password")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "人员管理", operationType = "重置密码", description = "重置检查员密码")
    public ApiResponse<Void> resetPassword(@PathVariable("id") Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if (!"INSPECTOR".equals(user.getRole())) {
            return ApiResponse.fail("只能重置检查员密码");
        }
        user.setPassword("{noop}123456");
        sysUserService.updateById(user);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/task-overview")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Map<String, Object>> getTaskOverview(@PathVariable("id") Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        String username = user.getUsername();

        // 该检查员分配到的所有任务（assignee字段可能包含逗号分隔的多个检查员）
        List<InspectionTask> tasks = taskService.lambdaQuery()
                .apply("FIND_IN_SET({0}, inspector) > 0", username)
                .orderByDesc(InspectionTask::getId)
                .list();

        // 统计完成情况
        long total = tasks.size();
        long completed = tasks.stream().filter(t -> "已完成".equals(t.getStatus())).count();
        long inProgress = tasks.stream().filter(t -> "进行中".equals(t.getStatus())).count();
        long pending = tasks.stream().filter(t -> "待审核".equals(t.getStatus())).count();

        // 检查记录数量
        List<Long> taskIds = tasks.stream().map(InspectionTask::getId).toList();
        long inspectionCount = 0;
        if (!taskIds.isEmpty()) {
            inspectionCount = recordService.lambdaQuery()
                    .in(InspectionRecord::getTaskId, taskIds)
                    .eq(InspectionRecord::getInspector, username)
                    .count();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("completed", completed);
        result.put("inProgress", inProgress);
        result.put("pending", pending);
        result.put("inspectionCount", inspectionCount);

        List<Map<String, Object>> taskList = new ArrayList<>();
        for (InspectionTask task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("name", task.getName());
            item.put("taskType", task.getTaskType());
            item.put("checkCategory", task.getCheckCategory());
            item.put("status", task.getStatus());
            item.put("dueDate", task.getDueDate());

            long recordCount = recordService.lambdaQuery()
                    .eq(InspectionRecord::getTaskId, task.getId())
                    .eq(InspectionRecord::getInspector, username)
                    .count();
            item.put("inspectionCount", recordCount);

            taskList.add(item);
        }
        result.put("tasks", taskList);

        return ApiResponse.ok(result);
    }
}
