package com.inspection.controller;

import com.inspection.common.annotation.OperationLog;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.Institution;
import com.inspection.entity.SysUser;
import com.inspection.entity.UserInstitution;
import com.inspection.service.InstitutionService;
import com.inspection.service.SysUserService;
import com.inspection.service.UserInstitutionService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {
    private final InstitutionService institutionService;
    private final UserInstitutionService userInstitutionService;
    private final SysUserService sysUserService;

    public InstitutionController(InstitutionService institutionService,
                                 UserInstitutionService userInstitutionService,
                                 SysUserService sysUserService) {
        this.institutionService = institutionService;
        this.userInstitutionService = userInstitutionService;
        this.sysUserService = sysUserService;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<Institution>> list() {
        List<Institution> list = institutionService.lambdaQuery()
                .orderByAsc(Institution::getId)
                .list();
        return ApiResponse.ok(list);
    }

    @PostMapping
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "人员管理", operationType = "新增", description = "新增检查机构")
    public ApiResponse<Institution> create(@RequestBody Institution institution) {
        institutionService.save(institution);
        return ApiResponse.ok("创建成功", institution);
    }

    @PutMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "人员管理", operationType = "修改", description = "修改检查机构")
    public ApiResponse<Institution> update(@PathVariable("id") Long id, @RequestBody Institution institution) {
        institution.setId(id);
        institutionService.updateById(institution);
        return ApiResponse.ok("更新成功", institution);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "人员管理", operationType = "删除", description = "删除检查机构")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        userInstitutionService.lambdaUpdate().eq(UserInstitution::getInstitutionId, id).remove();
        institutionService.removeById(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/inspectors")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<Map<String, Object>>> getInspectors(@PathVariable("id") Long id) {
        List<Long> userIds = userInstitutionService.lambdaQuery()
                .eq(UserInstitution::getInstitutionId, id)
                .list()
                .stream()
                .map(UserInstitution::getUserId)
                .toList();
        if (userIds.isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        List<Map<String, Object>> data = sysUserService.listByIds(userIds)
                .stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("realName", u.getRealName());
                    return m;
                })
                .toList();
        return ApiResponse.ok(data);
    }

    @PostMapping("/{id}/inspectors")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "人员管理", operationType = "分配", description = "分配检查员到机构")
    public ApiResponse<Integer> assignInspectors(@PathVariable("id") Long id, @RequestBody Map<String, Object> request) {
        Institution inst = institutionService.getById(id);
        if (inst == null) {
            return ApiResponse.fail("机构不存在");
        }
        @SuppressWarnings("unchecked")
        List<Number> userIds = (List<Number>) request.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            return ApiResponse.ok("未执行分配", 0);
        }
        int count = 0;
        for (Number uid : userIds) {
            long uidl = uid.longValue();
            long exists = userInstitutionService.lambdaQuery()
                    .eq(UserInstitution::getUserId, uidl)
                    .eq(UserInstitution::getInstitutionId, id)
                    .count();
            if (exists > 0) {
                continue;
            }
            UserInstitution ui = new UserInstitution();
            ui.setUserId(uidl);
            ui.setInstitutionId(id);
            userInstitutionService.save(ui);
            count++;
        }
        return ApiResponse.ok("分配成功", count);
    }

    @DeleteMapping("/{id}/inspectors/{userId}")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "人员管理", operationType = "移除", description = "移除机构下检查员")
    public ApiResponse<Void> removeInspector(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        userInstitutionService.lambdaUpdate()
                .eq(UserInstitution::getUserId, userId)
                .eq(UserInstitution::getInstitutionId, id)
                .remove();
        return ApiResponse.ok(null);
    }

    @PostMapping("/inspectors-by-institutions")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<List<Map<String, String>>> getInspectorsByInstitutions(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> institutionIds = (List<Number>) request.get("institutionIds");
        if (institutionIds == null || institutionIds.isEmpty()) {
            List<Map<String, String>> data = sysUserService.lambdaQuery()
                    .eq(SysUser::getRole, "INSPECTOR")
                    .eq(SysUser::getEnabled, true)
                    .orderByAsc(SysUser::getUsername)
                    .list()
                    .stream()
                    .map(u -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("username", u.getUsername());
                        m.put("realName", u.getRealName());
                        return m;
                    })
                    .toList();
            return ApiResponse.ok(data);
        }
        List<Long> instIds = institutionIds.stream().map(Number::longValue).toList();
        List<Long> userIds = userInstitutionService.lambdaQuery()
                .in(UserInstitution::getInstitutionId, instIds)
                .list()
                .stream()
                .map(UserInstitution::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        List<Map<String, String>> data = sysUserService.listByIds(userIds)
                .stream()
                .map(u -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("username", u.getUsername());
                    m.put("realName", u.getRealName());
                    return m;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(data);
    }
}
