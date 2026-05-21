package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.Institution;
import com.inspection.mapper.InstitutionMapper;
import com.inspection.service.InstitutionService;
import org.springframework.stereotype.Service;

@Service
public class InstitutionServiceImpl extends ServiceImpl<InstitutionMapper, Institution> implements InstitutionService {
}
