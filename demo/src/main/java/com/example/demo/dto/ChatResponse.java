package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for chat queries containing the generated answer,
 * source references, and metadata about retrieved chunks.
 * 
 * Validates Requirements 6.7, 7.2
 */
@Data
@Builder
@Schema(description = "Response containing the generated answer and source references")
public class ChatResponse {
    
    @Schema(
        description = "Generated answer based on retrieved document context",
        example = "The document discusses three main topics: machine learning fundamentals, neural network architectures, and practical applications in computer vision."
    )
    private String answer;
    
    @Schema(
        description = "List of source document chunks used to generate the answer",
        implementation = SourceReference.class
    )
    private List<SourceReference> sources;
    
    @Schema(
        description = "Total number of document chunks retrieved for this query",
        example = "5"
    )
    private int retrievedChunks;
}
