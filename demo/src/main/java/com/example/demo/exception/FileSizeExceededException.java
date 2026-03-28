package com.example.demo.exception;

/**
 * Exception thrown when uploaded file exceeds maximum size limit.
 * Results in HTTP 413 (Payload Too Large) response.
 */
public class FileSizeExceededException extends FileUploadException {
    
    public FileSizeExceededException(String message) {
        super(message);
    }
}
