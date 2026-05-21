package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.OperationLog;
import com.inspection.mapper.OperationLogMapper;
import com.inspection.service.OperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
}
