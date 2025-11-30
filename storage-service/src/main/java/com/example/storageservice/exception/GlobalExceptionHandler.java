package com.example.storageservice.exception;

import io.minio.errors.MinioException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("Validation error: ", exception);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException exception) {
        log.error("Method argument validation failed: ", exception);
        List<FieldError> fieldErrors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.add(FieldError.builder()
                    .field(error.getField())
                    .rejectedValue(error.getRejectedValue())
                    .message(error.getDefaultMessage())
                    .build());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .fieldErrors(fieldErrors)
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        log.error("Constraint violation: ", exception);
        List<FieldError> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            fieldErrors.add(FieldError.builder()
                    .field(violation.getPropertyPath().toString())
                    .rejectedValue(violation.getInvalidValue())
                    .message(violation.getMessage())
                    .build());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Request parameters validation failed")
                .fieldErrors(fieldErrors)
                .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        log.error("File size exceeded: ", exception);
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File Size Exceeded", "The uploaded file exceeds the maximum allowed size");
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException exception) {
        log.debug("Client aborted request: {}", exception.getMessage());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException exception) {
        log.debug("Async request not usable: {}", exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException exception) {
        if (exception.getMessage() != null && exception.getMessage().contains("Broken pipe")) {
            log.debug("Broken pipe (client disconnected): {}", exception.getMessage());
            return null;
        }
        log.error("IO Exception: ", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "IO Error", "Error processing file stream");
    }

    @ExceptionHandler(MinioException.class)
    public ResponseEntity<ErrorResponse> handleMinioException(MinioException exception) {
        log.error("MinIO Storage Exception: ", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Storage Error", "Error retrieving file from storage");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException exception) {
        log.error("Runtime exception occurred: ", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        log.error("Unexpected exception occurred: ", exception);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String error, String message) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValidationErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private List<FieldError> fieldErrors;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class FieldError {
        private String field;
        private Object rejectedValue;
        private String message;
    }
}
