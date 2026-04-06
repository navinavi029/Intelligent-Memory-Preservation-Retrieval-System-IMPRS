package com.example.demo.dto;

import com.example.demo.model.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Status information for how I'm doing with your shared memory.
 * Shows current status and any messages if I had trouble keeping it safe.
 * 
 * Validates Requirements 8.4
 */
@Data
@Builder
@Schema(description = "Status information for how I'm doing with your shared memory")
public class ProcessingStatusDto {
    
    @Schema(
        description = "Unique identifier for this memory",
        example = "123"
    )
    private Long documentId;
    
    @Schema(
        description = "Preview or title of your memory",
        example = "Had a wonderful visit with my grandchildren today..."
    )
    private String filename;
    
    @Schema(
        description = "How I'm doing with keeping your memory safe",
        example = "COMPLETED"
    )
    private ProcessingStatus status;
    
    @Schema(
        description = "How many pieces I organized your memory into",
        example = "42"
    )
    private Integer chunkCount;
    
    @Schema(
        description = "Any message if I had trouble with your memory, otherwise empty",
        example = "null",
        nullable = true
    )
    private String errorMessage;
    
    @Schema(
        description = "When you shared this memory with me",
        example = "2024-01-15T10:30:00"
    )
    private LocalDateTime uploadTimestamp;
}
