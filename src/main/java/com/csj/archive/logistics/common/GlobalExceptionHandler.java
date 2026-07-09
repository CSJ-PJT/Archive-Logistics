package com.csj.archive.logistics.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.status())
                .body(ErrorResponse.of(
                        exception.status().value(),
                        exception.code(),
                        exception.getMessage(),
                        List.of()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldViolation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toViolation)
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Request validation failed",
                        violations
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ErrorResponse.FieldViolation> violations = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Request validation failed",
                        violations
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_SERVER_ERROR",
                        exception.getMessage(),
                        List.of()
                ));
    }

    private ErrorResponse.FieldViolation toViolation(FieldError fieldError) {
        return new ErrorResponse.FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
