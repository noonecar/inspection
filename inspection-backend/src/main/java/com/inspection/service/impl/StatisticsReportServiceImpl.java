package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.entity.StatisticsReport;
import com.inspection.mapper.StatisticsReportMapper;
import com.inspection.service.StatisticsReportService;
import org.springframework.stereotype.Service;

@Service
public class StatisticsReportServiceImpl extends ServiceImpl<StatisticsReportMapper, StatisticsReport> implements StatisticsReportService {
}
