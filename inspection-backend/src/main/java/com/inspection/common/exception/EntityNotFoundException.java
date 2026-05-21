package com.inspection.common.exception;

public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String message) {
        super("ENTITY_NOT_FOUND", message);
    }
}
