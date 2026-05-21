package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@TableName("regulation_clause")
public class RegulationClause extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotNull(message = "法规ID不能为空")
    private Long regulationId;
    private String chapterTitle;
    @NotBlank(message = "条款编号不能为空")
    private String articleNo;
    @NotBlank(message = "条款分类不能为空")
    private String clauseCategory;
    private String keywordTag;
    @NotBlank(message = "条款内容不能为空")
    private String content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRegulationId() {
        return regulationId;
    }

    public void setRegulationId(Long regulationId) {
        this.regulationId = regulationId;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public String getArticleNo() {
        return articleNo;
    }

    public void setArticleNo(String articleNo) {
        this.articleNo = articleNo;
    }

    public String getClauseCategory() {
        return clauseCategory;
    }

    public void setClauseCategory(String clauseCategory) {
        this.clauseCategory = clauseCategory;
    }

    public String getKeywordTag() {
        return keywordTag;
    }

    public void setKeywordTag(String keywordTag) {
        this.keywordTag = keywordTag;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
