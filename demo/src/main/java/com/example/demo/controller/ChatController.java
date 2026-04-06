package com.example.demo.controller;

import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.dto.ErrorResponse;
import com.example.demo.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Your caring memory companion for exploring shared memories and stories.
 * Ask me anything about your memories and I'll help you recall the moments that matter most!
 * 
 * Validates Requirements 6.1, 12.2, 12.3
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Memory Chat", description = "Your caring memory companion - ask me anything about your shared memories and stories!")
public class ChatController {
    
    private final ChatService chatService;
    
    /**
     * Chat with me about your memories!
     * Just ask me a question and I'll search through all your shared memories to help you remember.
     * 
     * @param request Your question about your memories
     * @return A caring response to help you recall what matters most
     */
    @PostMapping("/query")
    @Operation(
        summary = "Chat with your memories",
        description = "Ask me anything about the memories you've shared! I'll search through " +
                     "them and give you a caring answer to help you remember. Think of it like " +
                     "having a conversation with someone who treasures all your precious moments."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Found some great info for you!",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ChatResponse.class),
                examples = @ExampleObject(
                    name = "Helpful response",
                    value = """
                        {
                          "answer": "Hey! I found some really interesting stuff in your research paper. It covers three main areas: machine learning fundamentals (which is super foundational), neural network architectures (the technical stuff), and practical applications in computer vision. Pretty comprehensive coverage!",
                          "sources": [
                            {
                              "documentId": 123,
                              "filename": "research-paper.pdf",
                              "chunkNumber": 5,
                              "similarityScore": 0.85
                            },
                            {
                              "documentId": 123,
                              "filename": "research-paper.pdf",
                              "chunkNumber": 12,
                              "similarityScore": 0.78
                            }
                          ],
                          "retrievedChunks": 5
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Oops, I need a bit more to work with",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Need more info",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "I need something to search for",
                          "path": "/api/chat/query",
                          "details": ["Could you ask me a question? I'm ready to help!"]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Something went wrong on my end",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ChatResponse.class),
                examples = @ExampleObject(
                    name = "Technical hiccup",
                    value = """
                        {
                          "answer": "Hmm, I'm having some trouble right now. Mind trying that again? Sometimes these things just need a second attempt.",
                          "sources": [],
                          "retrievedChunks": 0
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request) {
        log.info("Someone's asking about: {}", 
                request.getQuery().length() > 100 
                    ? request.getQuery().substring(0, 100) + "..." 
                    : request.getQuery());
        
        try {
            ChatResponse response = chatService.processQuery(request.getQuery());
            
            log.info("Found some good stuff! Sharing {} sources with them", 
                    response.getSources().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Oops, hit a snag while helping out: {}", e.getMessage(), e);
            
            // Return a friendly error response
            ChatResponse errorResponse = ChatResponse.builder()
                    .answer("Hmm, I'm having some trouble right now. Mind trying that again? Sometimes these things just need a second attempt.")
                    .sources(java.util.List.of())
                    .retrievedChunks(0)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
