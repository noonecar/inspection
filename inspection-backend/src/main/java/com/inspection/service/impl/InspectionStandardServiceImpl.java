package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.InspectionStandard;
import com.inspection.mapper.InspectionStandardMapper;
import com.inspection.service.InspectionStandardService;
import org.springframework.stereotype.Service;

@Service
public class InspectionStandardServiceImpl extends ServiceImpl<InspectionStandardMapper, InspectionStandard> implements InspectionStandardService {
}
