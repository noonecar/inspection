package com.inspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.annotation.OperationLog;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.FrequencyUsage;
import com.inspection.service.FrequencyUsageService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/frequencies")
public class FrequencyUsageController {
    private final FrequencyUsageService frequencyUsageService;

    public FrequencyUsageController(FrequencyUsageService frequencyUsageService) {
        this.frequencyUsageService = frequencyUsageService;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<FrequencyUsage>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                                  @RequestParam(name = "size", defaultValue = "10") long size,
                                                  @RequestParam(name = "keyword", required = false) String keyword,
                                                  @RequestParam(name = "stationId", required = false) Long stationId,
                                                  @RequestParam(name = "licenseNo", required = false) String licenseNo,
                                                  @RequestParam(name = "reviewStatus", required = false) String reviewStatus,
                                                  @RequestParam(name = "annualReportSubmitted", required = false) Boolean annualReportSubmitted,
                                                  @RequestParam(name = "spectrumFeePaid", required = false) Boolean spectrumFeePaid) {
        LambdaQueryWrapper<FrequencyUsage> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(FrequencyUsage::getLicenseNo, keyword)
                    .or().like(FrequencyUsage::getFrequencyRange, keyword)
                    .or().like(FrequencyUsage::getUsageRegion, keyword)
                    .or().like(FrequencyUsage::getBusinessUsage, keyword));
        }
        if (stationId != null) {
            wrapper.eq(FrequencyUsage::getStationId, stationId);
        }
        if (licenseNo != null && !licenseNo.isBlank()) {
            wrapper.like(FrequencyUsage::getLicenseNo, licenseNo);
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            wrapper.eq(FrequencyUsage::getReviewStatus, reviewStatus);
        }
        if (annualReportSubmitted != null) {
            wrapper.eq(FrequencyUsage::getAnnualReportSubmitted, annualReportSubmitted);
        }
        if (spectrumFeePaid != null) {
            wrapper.eq(FrequencyUsage::getSpectrumFeePaid, spectrumFeePaid);
        }

        List<FrequencyUsage> list = frequencyUsageService.list(wrapper.orderByDesc(FrequencyUsage::getId));
        Page<FrequencyUsage> res = new Page<>(page, size);
        res.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        res.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(res);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "数据管理-频率", operationType = "新增", description = "新增频率使用信息")
    public ApiResponse<FrequencyUsage> create(@Valid @RequestBody FrequencyUsage frequencyUsage) {
        frequencyUsageService.save(frequencyUsage);
        return ApiResponse.ok("创建成功", frequencyUsage);
    }

    @PutMapping("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "数据管理-频率", operationType = "修改", description = "修改频率使用信息")
    public ApiResponse<FrequencyUsage> update(@PathVariable("id") Long id, @Valid @RequestBody FrequencyUsage frequencyUsage) {
        frequencyUsage.setId(id);
        frequencyUsageService.updateById(frequencyUsage);
        return ApiResponse.ok("更新成功", frequencyUsage);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "数据管理-频率", operationType = "删除", description = "删除频率使用信息")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        frequencyUsageService.removeById(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/batch")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> batchCreate(@RequestBody List<@Valid FrequencyUsage> items) {
        if (items == null || items.isEmpty()) {
            return ApiResponse.ok("无数据", 0);
        }
        frequencyUsageService.saveBatch(items);
        return ApiResponse.ok("批量创建成功", items.size());
    }

    @PostMapping("/import-csv")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        List<FrequencyUsage> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] cols = line.split(",");
                if (cols.length < 9) {
                    continue;
                }
                FrequencyUsage item = new FrequencyUsage();
                item.setStationId(Long.parseLong(cols[0].trim()));
                item.setLicenseNo(cols[1].trim());
                item.setFrequencyRange(cols[2].trim());
                item.setUsageRegion(cols[3].trim());
                item.setBusinessUsage(cols[4].trim());
                item.setUsageDeadline(LocalDate.parse(cols[5].trim()));
                item.setUsageRate(new BigDecimal(cols[6].trim()));
                item.setAnnualReportSubmitted("1".equals(cols[7].trim()) || "true".equalsIgnoreCase(cols[7].trim()));
                item.setSpectrumFeePaid("1".equals(cols[8].trim()) || "true".equalsIgnoreCase(cols[8].trim()));
                item.setReviewStatus(cols.length > 9 ? cols[9].trim() : "待审核");
                imported.add(item);
            }
        }
        if (!imported.isEmpty()) {
            frequencyUsageService.saveBatch(imported);
        }
        return ApiResponse.ok("导入成功", imported.size());
    }

    @PostMapping("/review")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "数据管理-频率", operationType = "审核", description = "批量审核频率使用信息")
    public ApiResponse<Integer> review(@RequestBody Map<String, Object> request) {
        String reviewStatus = request.get("reviewStatus") == null ? "已审核" : request.get("reviewStatus").toString();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok("未执行审核", 0);
        }
        int updated = 0;
        for (Number id : ids) {
            FrequencyUsage freq = frequencyUsageService.getById(id.longValue());
            if (freq == null) {
                continue;
            }
            freq.setReviewStatus(reviewStatus);
            frequencyUsageService.updateById(freq);
            updated++;
        }
        return ApiResponse.ok("批量审核成功", updated);
    }

    @GetMapping("/export")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public void export(HttpServletResponse response) throws IOException {
        List<FrequencyUsage> list = frequencyUsageService.lambdaQuery().orderByDesc(FrequencyUsage::getId).list();
        StringBuilder csv = new StringBuilder("ID,台站ID,频率许可编号,使用频率,使用地域,业务用途,使用期限,使用率,EIRP谱密度,主站天线尺寸,年报,频占费,审核状态\n");
        for (FrequencyUsage freq : list) {
            csv.append(freq.getId()).append(',')
                    .append(freq.getStationId()).append(',')
                    .append(safeCsv(freq.getLicenseNo())).append(',')
                    .append(safeCsv(freq.getFrequencyRange())).append(',')
                    .append(safeCsv(freq.getUsageRegion())).append(',')
                    .append(safeCsv(freq.getBusinessUsage())).append(',')
                    .append(freq.getUsageDeadline() != null ? freq.getUsageDeadline().toString() : "").append(',')
                    .append(freq.getUsageRate() != null ? freq.getUsageRate().toString() : "").append(',')
                    .append(freq.getEirpSpectralDensity() != null ? freq.getEirpSpectralDensity().toString() : "").append(',')
                    .append(freq.getAntennaSize() != null ? freq.getAntennaSize().toString() : "").append(',')
                    .append(freq.getAnnualReportSubmitted() != null && freq.getAnnualReportSubmitted() ? "是" : "否").append(',')
                    .append(freq.getSpectrumFeePaid() != null && freq.getSpectrumFeePaid() ? "是" : "否").append(',')
                    .append(safeCsv(freq.getReviewStatus()))
                    .append("\n");
        }
        writeCsvResponse(response, "frequency-usage.csv", csv.toString());
    }

    private void writeCsvResponse(HttpServletResponse response, String filename, String content) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write('﻿');
            writer.write(content);
            writer.flush();
        }
    }

    private String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
