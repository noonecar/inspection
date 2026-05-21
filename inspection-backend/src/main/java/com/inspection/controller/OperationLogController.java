package com.inspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.OperationLog;
import com.inspection.service.OperationLogService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {
    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Page<OperationLog>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                                @RequestParam(name = "size", defaultValue = "10") long size,
                                                @RequestParam(name = "moduleName", required = false) String moduleName,
                                                @RequestParam(name = "operationType", required = false) String operationType,
                                                @RequestParam(name = "operator", required = false) String operator,
                                                @RequestParam(name = "startTime", required = false) String startTime,
                                                @RequestParam(name = "endTime", required = false) String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (moduleName != null && !moduleName.isBlank()) {
            wrapper.eq(OperationLog::getModuleName, moduleName);
        }
        if (operationType != null && !operationType.isBlank()) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        if (operator != null && !operator.isBlank()) {
            wrapper.like(OperationLog::getOperator, operator);
        }
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        List<OperationLog> list = operationLogService.list(wrapper.orderByDesc(OperationLog::getOperationTime));
        Page<OperationLog> res = new Page<>(page, size);
        res.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        res.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(res);
    }

    @GetMapping("/summary")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Map<String, Object>> summary() {
        List<OperationLog> logs = operationLogService.list();
        Map<String, Long> byOperationType = logs.stream()
                .collect(Collectors.groupingBy(OperationLog::getOperationType, Collectors.counting()));
        Map<String, Long> byModule = logs.stream()
                .collect(Collectors.groupingBy(OperationLog::getModuleName, Collectors.counting()));

        Map<String, Object> data = new HashMap<>();
        data.put("total", logs.size());
        data.put("byOperationType", byOperationType);
        data.put("byModule", byModule);
        return ApiResponse.ok(data);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        operationLogService.removeById(id);
        return ApiResponse.ok(null);
    }
}
