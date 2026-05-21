package com.inspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.annotation.OperationLog;
import com.inspection.common.result.ApiResponse;
import com.inspection.common.exception.BusinessException;
import com.inspection.dto.StationDetailDTO;
import com.inspection.entity.RadioStation;
import com.inspection.service.RadioStationService;
import jakarta.validation.Valid;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/stations")
public class RadioStationController {
    private final RadioStationService stationService;

    public RadioStationController(RadioStationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<RadioStation>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                                @RequestParam(name = "size", defaultValue = "10") long size,
                                                @RequestParam(name = "keyword", required = false) String keyword,
                                                @RequestParam(name = "stationClass", required = false) String stationClass,
                                                @RequestParam(name = "stationType", required = false) String stationType,
                                                @RequestParam(name = "serviceType", required = false) String serviceType,
                                                @RequestParam(name = "licenseNo", required = false) String licenseNo,
                                                @RequestParam(name = "category", required = false) String category,
                                                @RequestParam(name = "reviewStatus", required = false) String reviewStatus) {
        LambdaQueryWrapper<RadioStation> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(RadioStation::getName, keyword)
                    .or().like(RadioStation::getStationCode, keyword)
                    .or().like(RadioStation::getLicenseNo, keyword)
                    .or().like(RadioStation::getAddress, keyword));
        }
        if (stationClass != null && !stationClass.isBlank()) {
            wrapper.eq(RadioStation::getStationClass, stationClass);
        }
        if (stationType != null && !stationType.isBlank()) {
            wrapper.eq(RadioStation::getStationType, stationType);
        }
        if (serviceType != null && !serviceType.isBlank()) {
            wrapper.eq(RadioStation::getServiceType, serviceType);
        }
        if (licenseNo != null && !licenseNo.isBlank()) {
            wrapper.like(RadioStation::getLicenseNo, licenseNo);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(RadioStation::getCategory, category);
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            wrapper.eq(RadioStation::getReviewStatus, reviewStatus);
        }
        List<RadioStation> list = stationService.list(wrapper.orderByDesc(RadioStation::getId));
        Page<RadioStation> res = new Page<>(page, size);
        res.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        res.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(res);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "数据管理-台站", operationType = "新增", description = "新增台站信息")
    public ApiResponse<RadioStation> create(@Valid @RequestBody RadioStation station) throws BusinessException {
        return ApiResponse.ok("创建成功", stationService.createStation(station));
    }

    @PutMapping("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "数据管理-台站", operationType = "修改", description = "修改台站信息")
    public ApiResponse<RadioStation> update(@PathVariable("id") Long id, @Valid @RequestBody RadioStation station) throws BusinessException {
        return ApiResponse.ok("更新成功", stationService.updateStation(id, station));
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    @OperationLog(module = "数据管理-台站", operationType = "删除", description = "删除台站信息")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        stationService.removeById(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/detail")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<StationDetailDTO> detail(@PathVariable("id") Long id) throws BusinessException {
        return ApiResponse.ok(stationService.getStationDetail(id));
    }

    @PostMapping("/batch")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> batchCreate(@RequestBody List<@Valid RadioStation> items) throws BusinessException {
        if (items == null || items.isEmpty()) {
            return ApiResponse.ok("无数据", 0);
        }
        for (RadioStation item : items) {
            stationService.createStation(item);
        }
        return ApiResponse.ok("批量创建成功", items.size());
    }

    @PostMapping("/import-csv")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Integer> importCsv(@RequestParam("file") MultipartFile file) throws IOException, BusinessException {
        List<RadioStation> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] cols = line.split(",");
                if (cols.length < 8) {
                    continue;
                }
                RadioStation item = new RadioStation();
                item.setName(cols[0].trim());
                item.setStationClass(cols[1].trim());
                item.setCategory(cols[1].trim());
                item.setStationType(cols[2].trim());
                item.setServiceType(cols[3].trim());
                item.setStationCode(cols[4].trim());
                item.setAddress(cols[5].trim());
                item.setLicenseNo(cols[6].trim());
                item.setValidUntil(LocalDate.parse(cols[7].trim()));
                imported.add(item);
            }
        }
        for (RadioStation station : imported) {
            stationService.createStation(station);
        }
        return ApiResponse.ok("导入成功", imported.size());
    }

    @PostMapping("/review")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @OperationLog(module = "数据管理-台站", operationType = "审核", description = "审核台站信息")
    public ApiResponse<Integer> review(@RequestBody Map<String, Object> request) {
        String reviewStatus = request.get("reviewStatus") == null ? "已审核" : request.get("reviewStatus").toString();
        String reviewedBy = request.get("reviewedBy") == null ? "operator" : request.get("reviewedBy").toString();
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok("未执行审核", 0);
        }
        int updated = 0;
        for (Number id : ids) {
            RadioStation station = stationService.getById(id.longValue());
            if (station == null) {
                continue;
            }
            station.setReviewStatus(reviewStatus);
            station.setReviewedBy(reviewedBy);
            station.setReviewedAt(LocalDateTime.now());
            stationService.updateById(station);
            updated++;
        }
        return ApiResponse.ok("批量审核成功", updated);
    }

    @GetMapping("/export")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public void export(HttpServletResponse response) throws IOException {
        List<RadioStation> list = stationService.lambdaQuery().orderByDesc(RadioStation::getId).list();
        StringBuilder csv = new StringBuilder("台站ID,台站名称,附件分类,台站类型,业务类型,识别码,执照号,台址,审核状态\n");
        for (RadioStation station : list) {
            csv.append(station.getId()).append(',')
                    .append(safeCsv(station.getName())).append(',')
                    .append(safeCsv(station.getStationClass())).append(',')
                    .append(safeCsv(station.getStationType())).append(',')
                    .append(safeCsv(station.getServiceType())).append(',')
                    .append(safeCsv(station.getStationCode())).append(',')
                    .append(safeCsv(station.getLicenseNo())).append(',')
                    .append(safeCsv(station.getAddress())).append(',')
                    .append(safeCsv(station.getReviewStatus()))
                    .append("\n");
        }
        writeCsvResponse(response, "radio-stations.csv", csv.toString());
    }

    private void writeCsvResponse(HttpServletResponse response, String filename, String content) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
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
