package com.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("warning_history")
public class WarningHistory extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warningId;
    private String ruleName;
    private String level;
    private String message;
    private String status;
    private Long relatedTaskId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWarningId() { return warningId; }
    public void setWarningId(Long warningId) { this.warningId = warningId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getRelatedTaskId() { return relatedTaskId; }
    public void setRelatedTaskId(Long relatedTaskId) { this.relatedTaskId = relatedTaskId; }
}