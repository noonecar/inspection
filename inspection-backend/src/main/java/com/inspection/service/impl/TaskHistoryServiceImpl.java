package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.TaskHistory;
import com.inspection.mapper.TaskHistoryMapper;
import com.inspection.service.TaskHistoryService;
import org.springframework.stereotype.Service;

@Service
public class TaskHistoryServiceImpl extends ServiceImpl<TaskHistoryMapper, TaskHistory> implements TaskHistoryService {
}
