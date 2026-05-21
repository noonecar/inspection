package com.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inspection.common.exception.BusinessException;
import com.inspection.entity.InspectionTask;

public interface InspectionTaskService extends IService<InspectionTask> {
	InspectionTask createTask(InspectionTask task) throws BusinessException;

	InspectionTask updateTask(Long id, InspectionTask task) throws BusinessException;
}
