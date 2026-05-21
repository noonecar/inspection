package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("inspection_task")
public class InspectionTask extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("task_name")
    @NotBlank(message = "任务名称不能为空")
    private String name;
    @NotBlank(message = "任务类型不能为空")
    private String taskType;
    private String checkCategory;
    @NotBlank(message = "检查模式不能为空")
    private String inspectionMode;
    private String triggerSource;
    private Long stationId;
    private String stationIds;
    @NotBlank(message = "任务状态不能为空")
    private String status;
    @TableField("inspector")
    private String assignee;
    private LocalDate dueDate;
    private LocalDateTime remindedAt;
    private String cancelReason;
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getInspectionMode() {
        return inspectionMode;
    }

    public void setInspectionMode(String inspectionMode) {
        this.inspectionMode = inspectionMode;
    }

    public String getCheckCategory() {
        return checkCategory;
    }

    public void setCheckCategory(String checkCategory) {
        this.checkCategory = checkCategory;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public String getStationIds() {
        return stationIds;
    }

    public void setStationIds(String stationIds) {
        this.stationIds = stationIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getRemindedAt() {
        return remindedAt;
    }

    public void setRemindedAt(LocalDateTime remindedAt) {
        this.remindedAt = remindedAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
