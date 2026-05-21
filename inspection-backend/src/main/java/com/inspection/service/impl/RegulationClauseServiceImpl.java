package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.RegulationClause;
import com.inspection.mapper.RegulationClauseMapper;
import com.inspection.service.RegulationClauseService;
import org.springframework.stereotype.Service;

@Service
public class RegulationClauseServiceImpl extends ServiceImpl<RegulationClauseMapper, RegulationClause> implements RegulationClauseService {
}
