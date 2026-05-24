package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.WarningHistory;
import com.inspection.entity.WarningRecord;
import com.inspection.mapper.WarningRecordMapper;
import com.inspection.service.WarningHistoryService;
import com.inspection.service.WarningRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarningRecordServiceImpl extends ServiceImpl<WarningRecordMapper, WarningRecord> implements WarningRecordService {

    private final WarningHistoryService warningHistoryService;

    public WarningRecordServiceImpl(WarningHistoryService warningHistoryService) {
        this.warningHistoryService = warningHistoryService;
    }

    private void saveHistorySnapshot(WarningRecord record) {
        WarningHistory snapshot = new WarningHistory();
        snapshot.setWarningId(record.getId());
        snapshot.setRuleName(record.getRuleName());
        snapshot.setLevel(record.getLevel());
        snapshot.setMessage(record.getMessage());
        snapshot.setStatus(record.getStatus());
        snapshot.setRelatedTaskId(record.getRelatedTaskId());
        warningHistoryService.save(snapshot);
    }

    @Override
    @Transactional
    public int resolveWarningsByTaskId(Long taskId) {
        List<WarningRecord> warnings = lambdaQuery()
                .eq(WarningRecord::getRelatedTaskId, taskId)
                .ne(WarningRecord::getStatus, "已处理")
                .list();
        for (WarningRecord w : warnings) {
            w.setStatus("已处理");
        }
        if (!warnings.isEmpty()) {
            updateBatchById(warnings);
            for (WarningRecord w : warnings) {
                saveHistorySnapshot(w);
            }
        }
        return warnings.size();
    }

    @Override
    @Transactional
    public WarningRecord upsertByRuleAndTask(WarningRecord record) {
        WarningRecord existing = lambdaQuery()
                .eq(WarningRecord::getRuleName, record.getRuleName())
                .eq(WarningRecord::getRelatedTaskId, record.getRelatedTaskId())
                .one();
        if (existing != null) {
            // 如果预警已处理，避免重复保存"已处理"快照（resolveWarningsByTaskId已记录）
            // 改为保存更新后的快照，体现预警被重新触发
            if ("已处理".equals(existing.getStatus())) {
                record.setId(existing.getId());
                updateById(record);
                saveHistorySnapshot(record);
            } else {
                saveHistorySnapshot(existing);
                record.setId(existing.getId());
                updateById(record);
            }
            return record;
        } else {
            save(record);
            saveHistorySnapshot(record);
            return record;
        }
    }

    @Override
    @Transactional
    public void resolveWarning(Long id) {
        WarningRecord record = getById(id);
        if (record == null) {
            return;
        }
        record.setStatus("已处理");
        updateById(record);
        saveHistorySnapshot(record);
    }

    @Override
    @Transactional
    public void updateWarning(WarningRecord record) {
        WarningRecord existing = getById(record.getId());
        if (existing != null) {
            saveHistorySnapshot(existing);
        }
        updateById(record);
        if (existing != null) {
            saveHistorySnapshot(record);
        }
    }
}
