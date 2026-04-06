package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response containing a caring answer about your memories,
 * along with references to the specific memories that helped me respond.
 * 
 * Validates Requirements 6.7, 7.2
 */
@Data
@Builder
@Schema(description = "Response containing a caring answer about your memories and references to related stories")
public class ChatResponse {
    
    @Schema(
        description = "My caring response based on your shared memories",
        example = "From what you've shared, that family gathering was such a special day! You mentioned how everyone came together to celebrate your anniversary, and there were photos of three generations around the dinner table."
    )
    private String answer;
    
    @Schema(
        description = "References to the specific memories that helped me answer your question",
        implementation = SourceReference.class
    )
    private List<SourceReference> sources;
    
    @Schema(
        description = "Number of related memories I found to help answer your question",
        example = "5"
    )
    private int retrievedChunks;
}
