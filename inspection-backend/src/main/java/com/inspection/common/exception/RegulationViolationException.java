package com.inspection.common.exception;

public class RegulationViolationException extends BusinessException {
    public RegulationViolationException(String message) {
        super("REGULATION_VIOLATION", message);
    }
}
