package com.example.demo.dto;

import com.example.demo.model.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for document processing status endpoint.
 * Provides current status and any error messages if processing failed.
 * 
 * Validates Requirements 8.4
 */
@Data
@Builder
@Schema(description = "Processing status information for a document")
public class ProcessingStatusDto {
    
    @Schema(
        description = "Unique identifier for the document",
        example = "123"
    )
    private Long documentId;
    
    @Schema(
        description = "Current processing status",
        example = "COMPLETED"
    )
    private ProcessingStatus status;
    
    @Schema(
        description = "Number of chunks successfully processed",
        example = "42"
    )
    private Integer chunksProcessed;
    
    @Schema(
        description = "Error message if processing failed, null otherwise",
        example = "null",
        nullable = true
    )
    private String errorMessage;
}
