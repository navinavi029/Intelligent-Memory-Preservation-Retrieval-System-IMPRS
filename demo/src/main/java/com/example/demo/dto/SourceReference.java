package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Reference to a specific memory that helped me answer your question.
 * Includes relevance score to show how closely it matches what you asked about.
 * 
 * Validates Requirements 7.2
 */
@Data
@Builder
@Schema(description = "Reference to a specific memory that helped me answer your question")
public class SourceReference {
    
    @Schema(
        description = "ID of the memory that helped with your question",
        example = "123"
    )
    private Long documentId;
    
    @Schema(
        description = "Name or preview of the memory",
        example = "family-photos.pdf"
    )
    private String filename;
    
    @Schema(
        description = "Part of the memory that was most relevant",
        example = "5"
    )
    private Integer chunkNumber;
    
    @Schema(
        description = "How closely this memory matches your question (0.0 to 1.0, higher means more relevant)",
        example = "0.85"
    )
    private Double similarityScore;
}
