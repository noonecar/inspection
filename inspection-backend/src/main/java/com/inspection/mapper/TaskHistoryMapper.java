package com.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inspection.entity.TaskHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskHistoryMapper extends BaseMapper<TaskHistory> {
}
