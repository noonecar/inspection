package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("frequency_usage")
public class FrequencyUsage extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotNull(message = "台站ID不能为空")
    private Long stationId;
    @NotBlank(message = "频率许可编号不能为空")
    private String licenseNo;
    @NotBlank(message = "使用频率范围不能为空")
    private String frequencyRange;
    @NotBlank(message = "使用地域不能为空")
    private String usageRegion;
    @NotBlank(message = "业务用途不能为空")
    private String businessUsage;
    private LocalDate usageDeadline;
    private BigDecimal usageRate;
    private BigDecimal eirpSpectralDensity;
    private BigDecimal antennaSize;
    private Boolean annualReportSubmitted;
    private Boolean spectrumFeePaid;
    private Boolean interferenceFlag;
    private String technicalScheme;
    private String servicePersonnel;
    private String reviewStatus;
    private String evidenceUrls;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public String getFrequencyRange() {
        return frequencyRange;
    }

    public void setFrequencyRange(String frequencyRange) {
        this.frequencyRange = frequencyRange;
    }

    public String getUsageRegion() {
        return usageRegion;
    }

    public void setUsageRegion(String usageRegion) {
        this.usageRegion = usageRegion;
    }

    public String getBusinessUsage() {
        return businessUsage;
    }

    public void setBusinessUsage(String businessUsage) {
        this.businessUsage = businessUsage;
    }

    public LocalDate getUsageDeadline() {
        return usageDeadline;
    }

    public void setUsageDeadline(LocalDate usageDeadline) {
        this.usageDeadline = usageDeadline;
    }

    public BigDecimal getUsageRate() {
        return usageRate;
    }

    public void setUsageRate(BigDecimal usageRate) {
        this.usageRate = usageRate;
    }

    public BigDecimal getEirpSpectralDensity() {
        return eirpSpectralDensity;
    }

    public void setEirpSpectralDensity(BigDecimal eirpSpectralDensity) {
        this.eirpSpectralDensity = eirpSpectralDensity;
    }

    public BigDecimal getAntennaSize() {
        return antennaSize;
    }

    public void setAntennaSize(BigDecimal antennaSize) {
        this.antennaSize = antennaSize;
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

    public Boolean getInterferenceFlag() {
        return interferenceFlag;
    }

    public void setInterferenceFlag(Boolean interferenceFlag) {
        this.interferenceFlag = interferenceFlag;
    }

    public String getTechnicalScheme() {
        return technicalScheme;
    }

    public void setTechnicalScheme(String technicalScheme) {
        this.technicalScheme = technicalScheme;
    }

    public String getServicePersonnel() {
        return servicePersonnel;
    }

    public void setServicePersonnel(String servicePersonnel) {
        this.servicePersonnel = servicePersonnel;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(String evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }
}
