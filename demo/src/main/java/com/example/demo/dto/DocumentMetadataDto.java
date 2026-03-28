package com.example.demo.dto;

import com.example.demo.model.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO containing metadata about an uploaded document.
 * Used for listing documents and retrieving document information.
 * 
 * Validates Requirements 8.1, 8.5
 */
@Data
@Builder
@Schema(description = "Metadata information about an uploaded document")
public class DocumentMetadataDto {
    
    @Schema(
        description = "Unique identifier for the document",
        example = "123"
    )
    private Long id;
    
    @Schema(
        description = "Original filename of the uploaded document",
        example = "research-paper.pdf"
    )
    private String filename;
    
    @Schema(
        description = "File size in bytes",
        example = "2048576"
    )
    private Long fileSize;
    
    @Schema(
        description = "Timestamp when the document was uploaded",
        example = "2024-01-15T10:30:00"
    )
    private LocalDateTime uploadTimestamp;
    
    @Schema(
        description = "Current processing status of the document",
        example = "COMPLETED"
    )
    private ProcessingStatus status;
    
    @Schema(
        description = "Number of chunks the document was split into",
        example = "42"
    )
    private Integer chunkCount;
}
