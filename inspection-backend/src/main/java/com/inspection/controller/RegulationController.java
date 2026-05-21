package com.inspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.Regulation;
import com.inspection.service.RegulationService;
import jakarta.validation.Valid;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/regulations")
public class RegulationController {
    private final RegulationService regulationService;

    public RegulationController(RegulationService regulationService) {
        this.regulationService = regulationService;
    }

    @GetMapping
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<Regulation>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                              @RequestParam(name = "size", defaultValue = "10") long size,
                                              @RequestParam(name = "keyword", required = false) String keyword,
                                              @RequestParam(name = "category", required = false) String category,
                                              @RequestParam(name = "version", required = false) String version,
                                              @RequestParam(name = "startIssuedAt", required = false)
                                              @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startIssuedAt,
                                              @RequestParam(name = "endIssuedAt", required = false)
                                              @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endIssuedAt) {
        LambdaQueryWrapper<Regulation> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Regulation::getTitle, keyword)
                    .or().like(Regulation::getCategory, keyword)
                    .or().like(Regulation::getVersion, keyword));
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Regulation::getCategory, category);
        }
        if (version != null && !version.isBlank()) {
            wrapper.like(Regulation::getVersion, version);
        }
        if (startIssuedAt != null) {
            wrapper.ge(Regulation::getIssuedAt, startIssuedAt);
        }
        if (endIssuedAt != null) {
            wrapper.le(Regulation::getIssuedAt, endIssuedAt);
        }
        List<Regulation> list = regulationService.list(wrapper.orderByDesc(Regulation::getId));
        Page<Regulation> res = new Page<>(page, size);
        res.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        res.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(res);
    }

    @PostMapping
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Regulation> create(@Valid @RequestBody Regulation regulation) {
        regulationService.save(regulation);
        return ApiResponse.ok("创建成功", regulation);
    }

    @PutMapping("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<Regulation> update(@PathVariable("id") Long id, @Valid @RequestBody Regulation regulation) {
        regulation.setId(id);
        regulationService.updateById(regulation);
        return ApiResponse.ok("更新成功", regulation);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed({"ADMIN"})
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        regulationService.removeById(id);
        return ApiResponse.ok(null);
    }
}
