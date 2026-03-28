package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response DTO for all API errors.
 * Provides consistent error structure across all endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standardized error response for all API errors")
public class ErrorResponse {
    
    @Schema(
        description = "Timestamp when the error occurred",
        example = "2024-01-15T10:30:00"
    )
    private LocalDateTime timestamp;
    
    @Schema(
        description = "HTTP status code",
        example = "400"
    )
    private int status;
    
    @Schema(
        description = "Error type or category",
        example = "Bad Request"
    )
    private String error;
    
    @Schema(
        description = "Human-readable error message",
        example = "Query cannot be empty"
    )
    private String message;
    
    @Schema(
        description = "Request path that caused the error",
        example = "/api/chat/query"
    )
    private String path;
    
    @Schema(
        description = "Additional error details or validation messages",
        example = "[\"Query cannot be empty\", \"Query cannot exceed 1000 characters\"]"
    )
    private List<String> details;
}
