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
@Schema(description = "Request payload for chat queries against uploaded PDF documents")
public class ChatRequest {
    
    @NotBlank(message = "Query cannot be empty")
    @Size(max = 1000, message = "Query cannot exceed 1000 characters")
    @Schema(
        description = "Natural language question to ask about the uploaded documents",
        example = "What are the main topics discussed in the document?",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 1000
    )
    private String query;
}
