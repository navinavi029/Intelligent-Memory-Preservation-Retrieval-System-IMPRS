package com.example.demo.service;

import com.example.demo.dto.TextDocumentRequest;
import com.example.demo.model.Document;

/**
 * Service interface for diary entry processing operations.
 * Handles simple text entries for storage and retrieval.
 */
public interface TextProcessingService {
    
    /**
     * Process submitted diary entry:
     * - Validate entry content
     * - Create document record
     * - Store for retrieval (no chunking needed for single sentences)
     * 
     * @param request The diary entry request
     * @return Document entity with metadata and processing status
     * @throws IllegalArgumentException if entry validation fails
     * @throws RuntimeException if processing fails
     */
    Document processTextDocument(TextDocumentRequest request);
}