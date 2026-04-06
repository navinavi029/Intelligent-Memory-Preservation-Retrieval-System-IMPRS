package com.example.demo.dto;

import com.example.demo.model.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Information about a shared memory.
 * Used for listing memories and retrieving memory details.
 * 
 * Validates Requirements 8.1, 8.5
 */
@Data
@Builder
@Schema(description = "Information about a shared memory")
public class DocumentMetadataDto {
    
    @Schema(
        description = "Unique identifier for this memory",
        example = "123"
    )
    private Long id;
    
    @Schema(
        description = "Preview or title of your memory",
        example = "Had a wonderful visit with my grandchildren today..."
    )
    private String filename;
    
    @Schema(
        description = "Original name or preview of your memory",
        example = "Had a wonderful visit with my grandchildren today..."
    )
    private String originalFilename;
    
    @Schema(
        description = "Content length in characters (for text) or file size in bytes",
        example = "2048576"
    )
    private Long fileSize;
    
    @Schema(
        description = "When you shared this memory with me",
        example = "2024-01-15T10:30:00"
    )
    private LocalDateTime uploadTimestamp;
    
    @Schema(
        description = "How I'm doing with keeping your memory safe",
        example = "COMPLETED"
    )
    private ProcessingStatus status;
    
    @Schema(
        description = "How many pieces I organized your memory into for easy searching",
        example = "42"
    )
    private Integer chunkCount;
    
    @Schema(
        description = "Any message if I had trouble with your memory, otherwise empty",
        example = "null",
        nullable = true
    )
    private String errorMessage;
}
