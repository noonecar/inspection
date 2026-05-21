package com.inspection.common.enums;

public enum TaskStatus {
    PENDING_REVIEW("待审核"),
    IN_PROGRESS("进行中"),
    PENDING_REINSPECTION("待复检"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TaskStatus fromLabel(String label) {
        for (TaskStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        return null;
    }
}
