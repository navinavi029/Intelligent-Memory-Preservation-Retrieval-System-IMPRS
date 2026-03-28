package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * DTO representing a reference to a source document chunk used in generating a response.
 * Includes similarity score to indicate relevance.
 * 
 * Validates Requirements 7.2
 */
@Data
@Builder
@Schema(description = "Reference to a source document chunk used in generating the answer")
public class SourceReference {
    
    @Schema(
        description = "ID of the source document",
        example = "123"
    )
    private Long documentId;
    
    @Schema(
        description = "Original filename of the source document",
        example = "research-paper.pdf"
    )
    private String filename;
    
    @Schema(
        description = "Chunk number within the document (sequential)",
        example = "5"
    )
    private Integer chunkNumber;
    
    @Schema(
        description = "Cosine similarity score between query and chunk (0.0 to 1.0, higher is more relevant)",
        example = "0.85"
    )
    private Double similarityScore;
}
