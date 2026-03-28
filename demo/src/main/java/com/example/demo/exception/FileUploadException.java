package com.example.demo.exception;

/**
 * Exception thrown when file upload validation fails.
 * Used for file size and file type validation errors.
 */
public class FileUploadException extends RuntimeException {
    
    public FileUploadException(String message) {
        super(message);
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
