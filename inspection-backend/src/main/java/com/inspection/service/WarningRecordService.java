package com.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inspection.entity.WarningRecord;

public interface WarningRecordService extends IService<WarningRecord> {
    int resolveWarningsByTaskId(Long taskId);
    WarningRecord upsertByRuleAndTask(WarningRecord record);
    void resolveWarning(Long id);
    void updateWarning(WarningRecord record);
}
