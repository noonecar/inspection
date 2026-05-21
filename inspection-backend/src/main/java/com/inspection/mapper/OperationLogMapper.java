package com.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspection.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
