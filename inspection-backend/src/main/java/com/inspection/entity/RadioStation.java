package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("radio_station")
public class RadioStation extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stationId;
    @NotBlank(message = "台站名称不能为空")
    private String name;
    @NotBlank(message = "台站分类不能为空")
    private String category;
    @NotBlank(message = "附件分类不能为空")
    private String stationClass;
    @NotBlank(message = "台站类型不能为空")
    private String stationType;
    @NotBlank(message = "业务类型不能为空")
    private String serviceType;
    @NotBlank(message = "识别码不能为空")
    private String stationCode;
    private String address;
    private Double longitude;
    private Double latitude;
    private String licenseNo;
    private LocalDate validUntil;
    private BigDecimal transmitPower;
    private BigDecimal occupiedBandwidth;
    private BigDecimal antennaGain;
    private BigDecimal antennaHeight;
    private BigDecimal antennaSize;
    private String polarization;
    private BigDecimal maxEirp;
    private String orbitalPosition;
    private BigDecimal totalBandwidth;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedAt;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStationClass() {
        return stationClass;
    }

    public void setStationClass(String stationClass) {
        this.stationClass = stationClass;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public BigDecimal getTransmitPower() {
        return transmitPower;
    }

    public void setTransmitPower(BigDecimal transmitPower) {
        this.transmitPower = transmitPower;
    }

    public BigDecimal getOccupiedBandwidth() {
        return occupiedBandwidth;
    }

    public void setOccupiedBandwidth(BigDecimal occupiedBandwidth) {
        this.occupiedBandwidth = occupiedBandwidth;
    }

    public BigDecimal getAntennaGain() {
        return antennaGain;
    }

    public void setAntennaGain(BigDecimal antennaGain) {
        this.antennaGain = antennaGain;
    }

    public BigDecimal getAntennaHeight() {
        return antennaHeight;
    }

    public void setAntennaHeight(BigDecimal antennaHeight) {
        this.antennaHeight = antennaHeight;
    }

    public BigDecimal getAntennaSize() {
        return antennaSize;
    }

    public void setAntennaSize(BigDecimal antennaSize) {
        this.antennaSize = antennaSize;
    }

    public String getPolarization() {
        return polarization;
    }

    public void setPolarization(String polarization) {
        this.polarization = polarization;
    }

    public BigDecimal getMaxEirp() {
        return maxEirp;
    }

    public void setMaxEirp(BigDecimal maxEirp) {
        this.maxEirp = maxEirp;
    }

    public String getOrbitalPosition() {
        return orbitalPosition;
    }

    public void setOrbitalPosition(String orbitalPosition) {
        this.orbitalPosition = orbitalPosition;
    }

    public BigDecimal getTotalBandwidth() {
        return totalBandwidth;
    }

    public void setTotalBandwidth(BigDecimal totalBandwidth) {
        this.totalBandwidth = totalBandwidth;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
