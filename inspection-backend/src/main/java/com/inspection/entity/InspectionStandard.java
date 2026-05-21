package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;

@TableName("inspection_standard")
public class InspectionStandard extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "检查对象类型不能为空")
    private String objectType;
    private String stationType;
    @NotBlank(message = "检查项编码不能为空")
    private String itemCode;
    @NotBlank(message = "检查项名称不能为空")
    private String itemName;
    private String legalClause;
    private String checkType;
    private String checkMethod;
    private String judgmentRule;
    private String severityLevel;
    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getLegalClause() {
        return legalClause;
    }

    public void setLegalClause(String legalClause) {
        this.legalClause = legalClause;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getCheckMethod() {
        return checkMethod;
    }

    public void setCheckMethod(String checkMethod) {
        this.checkMethod = checkMethod;
    }

    public String getJudgmentRule() {
        return judgmentRule;
    }

    public void setJudgmentRule(String judgmentRule) {
        this.judgmentRule = judgmentRule;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
