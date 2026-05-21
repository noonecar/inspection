package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.WarningHistory;
import com.inspection.mapper.WarningHistoryMapper;
import com.inspection.service.WarningHistoryService;
import org.springframework.stereotype.Service;

@Service
public class WarningHistoryServiceImpl extends ServiceImpl<WarningHistoryMapper, WarningHistory> implements WarningHistoryService {
}