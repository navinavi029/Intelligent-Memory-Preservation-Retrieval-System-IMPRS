package com.example.demo.dto;

import com.example.demo.model.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Response for memory sharing operations.
 * Returns memory identifier, processing status, and caring messages.
 * 
 * Validates Requirements 1.6, 8.1
 */
@Data
@Builder
@Schema(description = "Response for memory sharing operations containing memory ID and processing status")
public class DocumentUploadResponse {
    
    @Schema(
        description = "Unique identifier for your shared memory",
        example = "123"
    )
    private Long documentId;
    
    @Schema(
        description = "Original name of your shared memory file",
        example = "family-photos.pdf"
    )
    private String filename;
    
    @Schema(
        description = "How I'm doing with processing your memory",
        example = "COMPLETED"
    )
    private ProcessingStatus status;
    
    @Schema(
        description = "A caring message about how your memory sharing went",
        example = "Your precious memories have been safely stored and are ready for you to explore anytime"
    )
    private String message;
}
