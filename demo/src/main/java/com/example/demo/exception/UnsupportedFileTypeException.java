package com.example.demo.exception;

/**
 * Exception thrown when uploaded file type is not supported.
 * Results in HTTP 415 (Unsupported Media Type) response.
 */
public class UnsupportedFileTypeException extends FileUploadException {
    
    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
