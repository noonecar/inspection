package com.inspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspection.common.result.ApiResponse;
import com.inspection.entity.InspectionStandard;
import com.inspection.entity.Regulation;
import com.inspection.entity.RegulationClause;
import com.inspection.service.InspectionStandardService;
import com.inspection.service.RegulationService;
import com.inspection.service.RegulationClauseService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/regulation-knowledge")
public class RegulationKnowledgeController {
    private final RegulationClauseService clauseService;
    private final InspectionStandardService standardService;
    private final RegulationService regulationService;

    @Value("${app.regulation.policy-doc-path:}")
    private String policyDocPath;

    @Value("${app.regulation.policy-doc-classpath:docs/台站核查.md}")
    private String policyDocClasspath;

    public RegulationKnowledgeController(RegulationClauseService clauseService,
                                         InspectionStandardService standardService,
                                         RegulationService regulationService) {
        this.clauseService = clauseService;
        this.standardService = standardService;
        this.regulationService = regulationService;
    }

    @GetMapping("/clauses")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<RegulationClause>> clauses(@RequestParam(name = "page", defaultValue = "1") long page,
                                                       @RequestParam(name = "size", defaultValue = "10") long size,
                                                       @RequestParam(name = "keyword", required = false) String keyword,
                                                       @RequestParam(name = "clauseCategory", required = false) String clauseCategory,
                                                       @RequestParam(name = "regulationId", required = false) Long regulationId) {
        LambdaQueryWrapper<RegulationClause> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(RegulationClause::getArticleNo, keyword)
                    .or().like(RegulationClause::getKeywordTag, keyword)
                    .or().like(RegulationClause::getContent, keyword)
                    .or().like(RegulationClause::getChapterTitle, keyword));
        }
        if (clauseCategory != null && !clauseCategory.isBlank()) {
            wrapper.eq(RegulationClause::getClauseCategory, clauseCategory);
        }
        if (regulationId != null) {
            wrapper.eq(RegulationClause::getRegulationId, regulationId);
        }

        List<RegulationClause> list = clauseService.list(wrapper.orderByDesc(RegulationClause::getId));
        Page<RegulationClause> result = new Page<>(page, size);
        result.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        result.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(result);
    }

    @PostMapping("/clauses")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<RegulationClause> createClause(@Valid @RequestBody RegulationClause clause) {
        clauseService.save(clause);
        return ApiResponse.ok("创建成功", clause);
    }

    @PutMapping("/clauses/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<RegulationClause> updateClause(@PathVariable("id") Long id, @Valid @RequestBody RegulationClause clause) {
        clause.setId(id);
        clauseService.updateById(clause);
        return ApiResponse.ok("更新成功", clause);
    }

    @GetMapping("/standards")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public ApiResponse<Page<InspectionStandard>> standards(@RequestParam(name = "page", defaultValue = "1") long page,
                                                           @RequestParam(name = "size", defaultValue = "10") long size,
                                                           @RequestParam(name = "keyword", required = false) String keyword,
                                                           @RequestParam(name = "objectType", required = false) String objectType,
                                                           @RequestParam(name = "severityLevel", required = false) String severityLevel) {
        LambdaQueryWrapper<InspectionStandard> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(InspectionStandard::getItemCode, keyword)
                    .or().like(InspectionStandard::getItemName, keyword)
                    .or().like(InspectionStandard::getLegalClause, keyword)
                    .or().like(InspectionStandard::getJudgmentRule, keyword));
        }
        if (objectType != null && !objectType.isBlank()) {
            wrapper.eq(InspectionStandard::getObjectType, objectType);
        }
        if (severityLevel != null && !severityLevel.isBlank()) {
            wrapper.eq(InspectionStandard::getSeverityLevel, severityLevel);
        }

        List<InspectionStandard> list = standardService.list(wrapper.orderByAsc(InspectionStandard::getItemCode));
        Page<InspectionStandard> result = new Page<>(page, size);
        result.setTotal(list.size());
        int fromIndex = (int) Math.max((page - 1) * size, 0);
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        result.setRecords(fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex));
        return ApiResponse.ok(result);
    }

    @PostMapping("/standards")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<InspectionStandard> createStandard(@Valid @RequestBody InspectionStandard standard) {
        standardService.save(standard);
        return ApiResponse.ok("创建成功", standard);
    }

    @PutMapping("/standards/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    public ApiResponse<InspectionStandard> updateStandard(@PathVariable("id") Long id, @Valid @RequestBody InspectionStandard standard) {
        standard.setId(id);
        standardService.updateById(standard);
        return ApiResponse.ok("更新成功", standard);
    }

    @PostMapping("/sync")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Map<String, Object>> syncLibrary() {
        String markdown = loadPolicyMarkdown();
        long targetRegulationId = resolveOrCreateTargetRegulationId();

        List<RegulationClause> clauses = extractChapterClauses(markdown, targetRegulationId);
        List<InspectionStandard> standards = extractInspectionStandards(markdown);

        clauseService.remove(new LambdaQueryWrapper<RegulationClause>().eq(RegulationClause::getRegulationId, targetRegulationId));
        standardService.remove(new LambdaQueryWrapper<InspectionStandard>().in(InspectionStandard::getObjectType, "无线电频率", "在用无线电台（站）"));

        if (!clauses.isEmpty()) {
            clauseService.saveBatch(clauses);
        }
        if (!standards.isEmpty()) {
            standardService.saveBatch(standards);
        }

        long clauseCount = clauses.size();
        long standardCount = standards.size();
        return ApiResponse.ok("法规条款与检查标准已同步", Map.of(
                "clauseCount", clauseCount,
                "standardCount", standardCount,
                "status", "SUCCESS"
        ));
    }

    private String loadPolicyMarkdown() {
        List<String> attempted = new ArrayList<>();

        if (policyDocPath != null && !policyDocPath.isBlank()) {
            Path configuredPath = Paths.get(policyDocPath.trim());
            attempted.add(configuredPath.toAbsolutePath().toString());
            try {
                if (Files.exists(configuredPath)) {
                    return Files.readString(configuredPath, StandardCharsets.UTF_8);
                }
            } catch (IOException ignored) {
            }
        }

        List<Path> candidates = List.of(
                Paths.get("..", "docs", "台站核查.md"),
                Paths.get("docs", "台站核查.md")
        );
        for (Path candidate : candidates) {
            attempted.add(candidate.toAbsolutePath().toString());
            try {
                if (Files.exists(candidate)) {
                    return Files.readString(candidate, StandardCharsets.UTF_8);
                }
            } catch (IOException ignored) {
            }
        }

        if (policyDocClasspath != null && !policyDocClasspath.isBlank()) {
            String classpathLocation = policyDocClasspath.startsWith("/")
                    ? policyDocClasspath.substring(1)
                    : policyDocClasspath;
            attempted.add("classpath:" + classpathLocation);
            try {
                ClassPathResource resource = new ClassPathResource(classpathLocation);
                if (resource.exists()) {
                    return resource.getContentAsString(StandardCharsets.UTF_8);
                }
            } catch (IOException ignored) {
            }
        }

        throw new IllegalStateException("未找到台站核查文档，无法同步法规知识库。已尝试: " + String.join(" | ", attempted));
    }

    private long resolveOrCreateTargetRegulationId() {
        String targetTitle = "无线电频率使用和在用无线电台（站）监督检查暂行办法";
        Regulation regulation = regulationService.lambdaQuery()
                .eq(Regulation::getTitle, targetTitle)
                .last("limit 1")
                .one();

        if (regulation != null && regulation.getId() != null) {
            return regulation.getId();
        }

        Regulation created = new Regulation();
        created.setTitle(targetTitle);
        created.setCategory("部门规章");
        created.setVersion("2022版");
        created.setFileUrl("miit.gov.cn/cms_files/filemanager/1226211233/attach/20224/cacfe2dbad9b475d93762093171bfe5d.pdf");
        created.setIssuedAt(java.time.LocalDate.of(2022, 5, 1));
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        regulationService.save(created);
        return created.getId();
    }

    private List<RegulationClause> extractChapterClauses(String markdown, long regulationId) {
        Set<String> allowedChapters = Set.of(
                "第一章 总则", "第二章 检查内容和要求", "第三章 检查的组织实施", "第四章 监督管理", "第五章 法律责任"
        );
        List<RegulationClause> clauses = new ArrayList<>();
        String[] lines = markdown.split("\\r?\\n");

        String currentChapter = "";
        String currentArticleNo = "";
        StringBuilder currentContent = new StringBuilder();

        Pattern articlePattern = Pattern.compile("^\\*\\*(第[一二三四五六七八九十百零〇]+条)\\*\\*\\s*(.*)$");

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.startsWith("## ")) {
                if (!currentArticleNo.isBlank() && allowedChapters.contains(currentChapter)) {
                    clauses.add(buildClause(regulationId, currentChapter, currentArticleNo, currentContent.toString()));
                    currentArticleNo = "";
                    currentContent.setLength(0);
                }
                currentChapter = line.substring(3).trim();
                continue;
            }

            Matcher matcher = articlePattern.matcher(line);
            if (matcher.matches()) {
                if (!currentArticleNo.isBlank() && allowedChapters.contains(currentChapter)) {
                    clauses.add(buildClause(regulationId, currentChapter, currentArticleNo, currentContent.toString()));
                    currentContent.setLength(0);
                }
                currentArticleNo = matcher.group(1);
                String firstLine = matcher.group(2) == null ? "" : matcher.group(2).trim();
                if (!firstLine.isBlank()) {
                    currentContent.append(firstLine);
                }
                continue;
            }

            if (!currentArticleNo.isBlank() && allowedChapters.contains(currentChapter)) {
                if (!line.isBlank()) {
                    if (!currentContent.isEmpty()) {
                        currentContent.append("\n");
                    }
                    currentContent.append(line);
                }
            }
        }

        if (!currentArticleNo.isBlank() && allowedChapters.contains(currentChapter)) {
            clauses.add(buildClause(regulationId, currentChapter, currentArticleNo, currentContent.toString()));
        }
        return clauses;
    }

    private RegulationClause buildClause(long regulationId, String chapterTitle, String articleNo, String content) {
        RegulationClause clause = new RegulationClause();
        clause.setRegulationId(regulationId);
        clause.setChapterTitle(trimToLength(chapterTitle, 255));
        clause.setArticleNo(trimToLength(articleNo, 64));
        clause.setClauseCategory(trimToLength(chapterTitle.replace("章", ""), 64));
        clause.setKeywordTag("");
        clause.setContent(content == null ? "" : content.trim());
        clause.setCreatedAt(LocalDateTime.now());
        clause.setUpdatedAt(LocalDateTime.now());
        return clause;
    }

    private List<InspectionStandard> extractInspectionStandards(String markdown) {
        Map<String, String> tableTitleMap = new LinkedHashMap<>();
        tableTitleMap.put("1-1", "地面无线电业务频率使用检查表");
        tableTitleMap.put("1-2", "卫星无线电频率使用检查表");
        tableTitleMap.put("1-3", "卫星通信网无线电频率使用检查表");
        tableTitleMap.put("2-1", "在用地面无线电业务台（站）检查表");
        tableTitleMap.put("2-2", "在用空间无线电台检查表");
        tableTitleMap.put("2-3", "在用卫星地球站检查表");

        List<InspectionStandard> standards = new ArrayList<>();
        String[] lines = markdown.split("\\r?\\n");
        Pattern tableHeaderPattern = Pattern.compile("^##\\s+([12]-[123])\\s+(.+)$");

        String currentTableCode = "";
        String currentTableTitle = "";
        Map<String, Integer> tableRowCounter = new HashMap<>();

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            Matcher tableMatcher = tableHeaderPattern.matcher(line);
            if (tableMatcher.matches()) {
                String code = tableMatcher.group(1);
                if (tableTitleMap.containsKey(code)) {
                    currentTableCode = code;
                    currentTableTitle = tableMatcher.group(2).trim();
                    tableRowCounter.putIfAbsent(code, 0);
                } else {
                    currentTableCode = "";
                    currentTableTitle = "";
                }
                continue;
            }

            if (currentTableCode.isBlank() || !line.startsWith("|")) {
                continue;
            }

            List<String> cells = splitMarkdownRow(line);
            if (cells.size() < 5) {
                continue;
            }
            if ("检查内容".equals(cells.get(0)) || isSeparatorRow(cells)) {
                continue;
            }

            int index = tableRowCounter.getOrDefault(currentTableCode, 0) + 1;
            tableRowCounter.put(currentTableCode, index);

            String checkContent = cells.get(0);
            String subItem = cells.get(1);
            String detail = cells.get(2);
            String checkType = cells.get(3);
            String checkMethod = cells.get(4);

            InspectionStandard standard = new InspectionStandard();
            standard.setObjectType(currentTableCode.startsWith("1-") ? "无线电频率" : "在用无线电台（站）");
            standard.setStationType(trimToLength(currentTableTitle, 128));
            standard.setItemCode(trimToLength(currentTableCode + "-" + String.format("%02d", index), 64));
            standard.setItemName(trimToLength(buildItemName(checkContent, subItem), 255));
            standard.setLegalClause(currentTableCode.startsWith("1-") ? "第八条" : "第九条");
            standard.setCheckType(trimToLength(checkType, 128));
            standard.setCheckMethod(trimToLength(checkMethod, 512));
            standard.setJudgmentRule(trimToLength(detail, 512));
            standard.setSeverityLevel("中");
            standard.setEnabled(true);
            standard.setCreatedAt(LocalDateTime.now());
            standard.setUpdatedAt(LocalDateTime.now());
            standards.add(standard);
        }
        return standards;
    }

    private List<String> splitMarkdownRow(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String[] arr = trimmed.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String item : arr) {
            cells.add(item == null ? "" : item.trim());
        }
        return cells;
    }

    private boolean isSeparatorRow(List<String> cells) {
        for (String cell : cells) {
            if (!cell.matches("[-:]+")) {
                return false;
            }
        }
        return true;
    }

    private String buildItemName(String checkContent, String subItem) {
        if (subItem == null || subItem.isBlank() || "-".equals(subItem)) {
            return checkContent;
        }
        return checkContent + " / " + subItem;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    @GetMapping("/download")
    @RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})
    public void download(@RequestParam(name = "type", defaultValue = "clauses") String type,
                         HttpServletResponse response) throws IOException {
        if ("standards".equals(type)) {
            List<InspectionStandard> list = standardService.list();
            StringBuilder csv = new StringBuilder("ID,对象类型,台站类型,项号,检查项,法规条款,检查方式,检查方法,判定规则,风险等级\n");
            for (InspectionStandard s : list) {
                csv.append(s.getId()).append(',')
                        .append(safeCsv(s.getObjectType())).append(',')
                        .append(safeCsv(s.getStationType())).append(',')
                        .append(safeCsv(s.getItemCode())).append(',')
                        .append(safeCsv(s.getItemName())).append(',')
                        .append(safeCsv(s.getLegalClause())).append(',')
                        .append(safeCsv(s.getCheckType())).append(',')
                        .append(safeCsv(s.getCheckMethod())).append(',')
                        .append(safeCsv(s.getJudgmentRule())).append(',')
                        .append(safeCsv(s.getSeverityLevel())).append('\n');
            }
            writeCsvResponse(response, "inspection-standards.csv", csv.toString());
            return;
        }

        List<RegulationClause> clauses = clauseService.list();
        StringBuilder csv = new StringBuilder("ID,法规ID,章节,条款编号,分类,关键词,条款内容\n");
        for (RegulationClause c : clauses) {
            csv.append(c.getId()).append(',')
                    .append(c.getRegulationId()).append(',')
                    .append(safeCsv(c.getChapterTitle())).append(',')
                    .append(safeCsv(c.getArticleNo())).append(',')
                    .append(safeCsv(c.getClauseCategory())).append(',')
                    .append(safeCsv(c.getKeywordTag())).append(',')
                    .append(safeCsv(c.getContent())).append('\n');
        }
        writeCsvResponse(response, "regulation-clauses.csv", csv.toString());
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
