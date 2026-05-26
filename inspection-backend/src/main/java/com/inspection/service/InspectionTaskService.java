package com.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.inspection.common.exception.BusinessException;
import com.inspection.entity.InspectionTask;

import java.util.Arrays;

public interface InspectionTaskService extends IService<InspectionTask> {
	InspectionTask createTask(InspectionTask task) throws BusinessException;

	InspectionTask updateTask(Long id, InspectionTask task) throws BusinessException;

	/**
	 * 对 LambdaQueryWrapper 应用 FIND_IN_SET 过滤条件，
	 * 检查指定 username 是否在 inspector 字段的逗号分隔列表中
	 */
	default void applyInspectorFilter(LambdaQueryWrapper<InspectionTask> wrapper, String username) {
		wrapper.apply("FIND_IN_SET({0}, inspector) > 0", username);
	}

	/**
	 * 检查 username 是否在逗号分隔的 assignee 列表中（内存中判断）
	 */
	default boolean isInspectorAssigned(String assignee, String username) {
		if (assignee == null || username == null) return false;
		return Arrays.asList(assignee.split(",")).contains(username);
	}
}
