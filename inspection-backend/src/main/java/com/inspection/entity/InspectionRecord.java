package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("inspection_record")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionRecord extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    @NotNull(message = "台站ID不能为空")
    private Long stationId;
    @NotBlank(message = "对象类型不能为空")
    private String objectType;
    @NotBlank(message = "检查项编码不能为空")
    private String itemCode;
    @NotBlank(message = "检查项名称不能为空")
    private String itemName;
    @NotBlank(message = "检查方式不能为空")
    private String checkType;
    @NotBlank(message = "检查方法不能为空")
    private String checkMethod;
    @NotBlank(message = "法规依据不能为空")
    private String legalBasis;
    @NotBlank(message = "检查模式不能为空")
    private String inspectionMode;
    private String triggerSource;
    private String stationCategory;
    @NotBlank(message = "检查结果不能为空")
    private String result;
    private Boolean needsRectification;
    private LocalDate rectificationDeadline;
    private Boolean annualReportSubmitted;
    private Boolean spectrumFeePaid;
    private Boolean technicalPersonnelOk;
    private Boolean harmfulInterference;
    private Boolean increasedFrequencyRequired;
    private String processRecord;
    private String evidenceUrls;
    private String auditStatus;
    private String remarks;
    @NotBlank(message = "检查员不能为空")
    private String inspector;
    @TableField("inspection_round")
    private Integer round;
    @TableField(exist = false)
    private String warningRuleDetail;
    @NotNull(message = "检查时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
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

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public String getInspectionMode() {
        return inspectionMode;
    }

    public void setInspectionMode(String inspectionMode) {
        this.inspectionMode = inspectionMode;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getStationCategory() {
        return stationCategory;
    }

    public void setStationCategory(String stationCategory) {
        this.stationCategory = stationCategory;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Boolean getNeedsRectification() {
        return needsRectification;
    }

    public void setNeedsRectification(Boolean needsRectification) {
        this.needsRectification = needsRectification;
    }

    public LocalDate getRectificationDeadline() {
        return rectificationDeadline;
    }

    public void setRectificationDeadline(LocalDate rectificationDeadline) {
        this.rectificationDeadline = rectificationDeadline;
    }

    public Boolean getAnnualReportSubmitted() {
        return annualReportSubmitted;
    }

    public void setAnnualReportSubmitted(Boolean annualReportSubmitted) {
        this.annualReportSubmitted = annualReportSubmitted;
    }

    public Boolean getSpectrumFeePaid() {
        return spectrumFeePaid;
    }

    public void setSpectrumFeePaid(Boolean spectrumFeePaid) {
        this.spectrumFeePaid = spectrumFeePaid;
    }

    public Boolean getTechnicalPersonnelOk() {
        return technicalPersonnelOk;
    }

    public void setTechnicalPersonnelOk(Boolean technicalPersonnelOk) {
        this.technicalPersonnelOk = technicalPersonnelOk;
    }

    public Boolean getHarmfulInterference() {
        return harmfulInterference;
    }

    public void setHarmfulInterference(Boolean harmfulInterference) {
        this.harmfulInterference = harmfulInterference;
    }

    public Boolean getIncreasedFrequencyRequired() {
        return increasedFrequencyRequired;
    }

    public void setIncreasedFrequencyRequired(Boolean increasedFrequencyRequired) {
        this.increasedFrequencyRequired = increasedFrequencyRequired;
    }

    public String getProcessRecord() {
        return processRecord;
    }

    public void setProcessRecord(String processRecord) {
        this.processRecord = processRecord;
    }

    public String getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(String evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getInspector() {
        return inspector;
    }

    public void setInspector(String inspector) {
        this.inspector = inspector;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public String getWarningRuleDetail() {
        return warningRuleDetail;
    }

    public void setWarningRuleDetail(String warningRuleDetail) {
        this.warningRuleDetail = warningRuleDetail;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
