package com.csj.archive.logistics.common;

import org.springframework.http.HttpStatus;

public class DuplicateEventException extends BusinessException {
    public DuplicateEventException(String message) {
        super("DUPLICATE_EVENT", message, HttpStatus.ACCEPTED);
    }
}
