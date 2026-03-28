package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for centralizing error handling across all controllers.
 * Handles validation exceptions, file upload exceptions, resource not found exceptions,
 * and general exceptions with standardized error responses.
 * 
 * Validates Requirements 9.4, 12.5
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle validation exceptions from Bean Validation.
     * Returns HTTP 400 with field-specific error messages.
     * 
     * @param ex The validation exception
     * @param request The HTTP request
     * @return ErrorResponse with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .details(details)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle malformed JSON and message not readable exceptions.
     * Returns HTTP 400 (Bad Request).
     * 
     * @param ex The message not readable exception
     * @param request The HTTP request
     * @return ErrorResponse with descriptive message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        log.warn("Malformed request body for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Malformed request body")
                .path(request.getRequestURI())
                .details(List.of("Request body could not be read or parsed"))
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle file size exceeded exceptions.
     * Returns HTTP 413 (Payload Too Large).
     * 
     * @param ex The file size exception
     * @param request The HTTP request
     * @return ErrorResponse with descriptive message
     */
    @ExceptionHandler({FileSizeExceededException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorResponse> handleFileSizeExceededException(
            Exception ex,
            HttpServletRequest request) {
        
        log.warn("File size exceeded for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase())
                .message("File size exceeds maximum allowed size of 10MB")
                .path(request.getRequestURI())
                .details(List.of(ex.getMessage()))
                .build();
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }
    
    /**
     * Handle unsupported file type exceptions.
     * Returns HTTP 415 (Unsupported Media Type).
     * 
     * @param ex The unsupported file type exception
     * @param request The HTTP request
     * @return ErrorResponse with descriptive message
     */
    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFileTypeException(
            UnsupportedFileTypeException ex,
            HttpServletRequest request) {
        
        log.warn("Unsupported file type for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
                .message("Only PDF files are supported")
                .path(request.getRequestURI())
                .details(List.of(ex.getMessage()))
                .build();
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }
    
    /**
     * Handle resource not found exceptions.
     * Returns HTTP 404 (Not Found).
     * 
     * @param ex The resource not found exception
     * @param request The HTTP request
     * @return ErrorResponse with descriptive message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Resource not found for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(new ArrayList<>())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle general file upload exceptions.
     * Returns HTTP 400 (Bad Request).
     * 
     * @param ex The file upload exception
     * @param request The HTTP request
     * @return ErrorResponse with descriptive message
     */
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(
            FileUploadException ex,
            HttpServletRequest request) {
        
        log.warn("File upload error for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("File upload failed")
                .path(request.getRequestURI())
                .details(List.of(ex.getMessage()))
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle all other uncaught exceptions.
     * Returns HTTP 500 (Internal Server Error).
     * 
     * @param ex The exception
     * @param request The HTTP request
     * @return ErrorResponse with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error for request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .details(List.of(ex.getMessage()))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
