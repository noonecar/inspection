package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.FrequencyUsage;
import com.inspection.mapper.FrequencyUsageMapper;
import com.inspection.service.FrequencyUsageService;
import org.springframework.stereotype.Service;

@Service
public class FrequencyUsageServiceImpl extends ServiceImpl<FrequencyUsageMapper, FrequencyUsage> implements FrequencyUsageService {
}
