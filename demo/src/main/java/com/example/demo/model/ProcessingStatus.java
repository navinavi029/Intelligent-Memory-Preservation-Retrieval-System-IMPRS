package com.example.demo.model;

/**
 * Enum representing the processing status of uploaded PDF documents.
 * Tracks the document through the extraction, chunking, and embedding pipeline.
 * 
 * Validates Requirements 1.6, 8.5
 */
public enum ProcessingStatus {
    /**
     * Document has been uploaded but processing has not started
     */
    PENDING,
    
    /**
     * Document is currently being processed (extraction, chunking, embedding)
     */
    PROCESSING,
    
    /**
     * Document processing completed successfully
     */
    COMPLETED,
    
    /**
     * Document processing failed due to an error
     */
    FAILED
}
