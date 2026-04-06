package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for asking questions about your shared memories and personal stories")
public class ChatRequest {
    
    @NotBlank(message = "Please share what you'd like to remember")
    @Size(max = 1000, message = "Let's keep your question under 1000 characters")
    @Schema(
        description = "Your question about memories you've shared with me",
        example = "Tell me about the family gathering we talked about",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 1000
    )
    private String query;
}
