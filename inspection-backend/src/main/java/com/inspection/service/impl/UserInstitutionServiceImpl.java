package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.UserInstitution;
import com.inspection.mapper.UserInstitutionMapper;
import com.inspection.service.UserInstitutionService;
import org.springframework.stereotype.Service;

@Service
public class UserInstitutionServiceImpl extends ServiceImpl<UserInstitutionMapper, UserInstitution> implements UserInstitutionService {
}
