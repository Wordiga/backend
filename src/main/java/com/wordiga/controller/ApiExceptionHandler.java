package com.wordiga.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ApiError> handleValidation(BindException exception) {
        List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage())).toList();
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값을 확인해 주세요.", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldError> fields = exception.getConstraintViolations().stream()
                .map(error -> new FieldError(error.getPropertyPath().toString(), error.getMessage())).toList();
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값을 확인해 주세요.", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleUnreadable(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값을 확인해 주세요.", List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return error(status, code(status, exception.getReason()), exception.getReason(), List.of());
    }

    private String code(HttpStatus status, String reason) {
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            if (reason != null && reason.contains("관광공사")) return "TOURISM_API_UNAVAILABLE";
            if (reason != null && reason.contains("제안서")) return "PROPOSAL_STORAGE_UNAVAILABLE";
            return "AI_SERVER_UNAVAILABLE";
        }
        if (status == HttpStatus.NOT_FOUND)
            return reason != null && reason.contains("일정") ? "PLAN_NOT_FOUND" : "CONTENT_NOT_FOUND";
        return switch (status) {
            case BAD_GATEWAY -> "AI_RESPONSE_INVALID";
            default -> "INVALID_REQUEST";
        };
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, List<FieldError> fields) {
        return ResponseEntity.status(status).body(new ApiError(code, message, fields));
    }

    public record ApiError(String code, String message, List<FieldError> fieldErrors) { }
    public record FieldError(String field, String reason) { }
}
