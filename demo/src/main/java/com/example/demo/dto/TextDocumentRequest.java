package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request for sharing a precious memory moment.
 * Contains a meaningful memory entry for safekeeping and future recall.
 */
@Data
@Schema(description = "Request for sharing a precious memory moment or meaningful story")
public class TextDocumentRequest {
    
    @NotBlank(message = "Please share a memory with me")
    @Size(max = 500, message = "Let's keep your memory under 500 characters so it's easy to remember")
    @Schema(
        description = "A precious memory moment or meaningful story you'd like me to remember",
        example = "Had a wonderful visit with my grandchildren today. We baked cookies together and they told me all about their school adventures.",
        required = true
    )
    private String memory;
}