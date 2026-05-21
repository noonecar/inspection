package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.Regulation;
import com.inspection.mapper.RegulationMapper;
import com.inspection.service.RegulationService;
import org.springframework.stereotype.Service;

@Service
public class RegulationServiceImpl extends ServiceImpl<RegulationMapper, Regulation> implements RegulationService {
}
