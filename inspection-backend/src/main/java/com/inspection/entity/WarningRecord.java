package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;

@TableName("warning_record")
public class WarningRecord extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "预警规则不能为空")
    private String ruleName;
    @NotBlank(message = "预警等级不能为空")
    private String level;
    @NotBlank(message = "预警内容不能为空")
    private String message;
    @NotBlank(message = "预警状态不能为空")
    private String status;
    private Long relatedTaskId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRelatedTaskId() {
        return relatedTaskId;
    }

    public void setRelatedTaskId(Long relatedTaskId) {
        this.relatedTaskId = relatedTaskId;
    }
}
