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
 * REST controller for handling chat query requests.
 * Provides endpoint for querying uploaded PDF documents using natural language.
 * 
 * Validates Requirements 6.1, 12.2, 12.3
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Endpoints for querying uploaded PDF documents using natural language")
public class ChatController {
    
    private final ChatService chatService;
    
    /**
     * Process a chat query against uploaded documents.
     * Validates request using Bean Validation and returns answer with source references.
     * 
     * @param request The chat request containing the user query
     * @return ChatResponse with answer and sources
     */
    @PostMapping("/query")
    @Operation(
        summary = "Query uploaded documents",
        description = "Process a natural language question against uploaded PDF documents. " +
                     "The system retrieves relevant document chunks using semantic search and " +
                     "generates an answer using Google Gemini API. Returns the answer along with " +
                     "source references indicating which document chunks were used."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Query processed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ChatResponse.class),
                examples = @ExampleObject(
                    name = "Successful query response",
                    value = """
                        {
                          "answer": "The document discusses three main topics: machine learning fundamentals, neural network architectures, and practical applications in computer vision.",
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
            description = "Invalid request - query is empty or exceeds maximum length",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Validation failed",
                          "path": "/api/chat/query",
                          "details": ["Query cannot be empty"]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - failed to process query or communicate with Gemini API",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ChatResponse.class),
                examples = @ExampleObject(
                    name = "Processing error",
                    value = """
                        {
                          "answer": "An error occurred while processing your query. Please try again.",
                          "sources": [],
                          "retrievedChunks": 0
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ChatResponse> query(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat query: {}", 
                request.getQuery().length() > 100 
                    ? request.getQuery().substring(0, 100) + "..." 
                    : request.getQuery());
        
        try {
            ChatResponse response = chatService.processQuery(request.getQuery());
            
            log.info("Successfully processed query, returned {} sources", 
                    response.getSources().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to process chat query: {}", e.getMessage(), e);
            
            // Return error response with appropriate status
            ChatResponse errorResponse = ChatResponse.builder()
                    .answer("An error occurred while processing your query. Please try again.")
                    .sources(java.util.List.of())
                    .retrievedChunks(0)
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
