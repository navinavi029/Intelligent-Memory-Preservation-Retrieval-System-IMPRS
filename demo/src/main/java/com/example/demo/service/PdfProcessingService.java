package com.example.demo.service;

import com.example.demo.model.Document;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for PDF document processing operations.
 * Orchestrates the workflow of PDF text extraction, validation, and document management.
 * 
 * Validates Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 12.1
 */
public interface PdfProcessingService {
    
    /**
     * Process uploaded PDF file through the complete workflow:
     * - Validate file size and type
     * - Extract text content
     * - Update document status throughout processing
     * 
     * This method orchestrates the entire PDF processing pipeline but does not
     * handle chunking or embedding generation (handled by separate services).
     * 
     * @param file The uploaded PDF file
     * @return Document entity with metadata and processing status
     * @throws IllegalArgumentException if file validation fails
     * @throws RuntimeException if text extraction fails
     */
    Document processDocument(MultipartFile file);
    
    /**
     * Extract text content from a PDF file using Spring AI PDF document reader.
     * 
     * @param file The PDF file to extract text from
     * @return Extracted text content as a single string
     * @throws RuntimeException if text extraction fails
     */
    String extractText(MultipartFile file);
}
