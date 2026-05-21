package com.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inspection.common.exception.BusinessException;
import com.inspection.entity.InspectionRecord;

public interface InspectionRecordService extends IService<InspectionRecord> {
	InspectionRecord createRecord(InspectionRecord record) throws BusinessException;

	InspectionRecord updateRecord(Long id, InspectionRecord record) throws BusinessException;

	void recalculateTaskStatus(Long taskId);
}
