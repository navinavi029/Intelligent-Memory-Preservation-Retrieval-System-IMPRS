package com.example.demo.dto;

import com.example.demo.model.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for document upload operations.
 * Returns document identifier, processing status, and any relevant messages.
 * 
 * Validates Requirements 1.6, 8.1
 */
@Data
@Builder
@Schema(description = "Response for document upload operations containing document ID and processing status")
public class DocumentUploadResponse {
    
    @Schema(
        description = "Unique identifier for the uploaded document",
        example = "123"
    )
    private Long documentId;
    
    @Schema(
        description = "Original filename of the uploaded document",
        example = "research-paper.pdf"
    )
    private String filename;
    
    @Schema(
        description = "Current processing status of the document",
        example = "COMPLETED"
    )
    private ProcessingStatus status;
    
    @Schema(
        description = "Descriptive message about the upload result or any errors",
        example = "Document uploaded and processed successfully"
    )
    private String message;
}
