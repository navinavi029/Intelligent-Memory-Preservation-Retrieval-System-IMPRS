package com.example.demo.controller;

import com.example.demo.repository.ChunkRepository;
import com.example.demo.repository.DocumentRepository;
import com.example.demo.service.EmbeddingService;
import com.example.demo.service.ResilientNvidiaChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Diagnostic controller to test individual components.
 */
@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticController {
    
    private final EmbeddingService embeddingService;
    private final ResilientNvidiaChatClient resilientNvidiaChatClient;
    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        log.info("[DiagnosticController] Testing database connectivity");
        
        try {
            // Test basic connection first
            log.info("[DiagnosticController] Testing basic database connection");
            long documentCount = documentRepository.count();
            log.info("[DiagnosticController] Document count query successful: {}", documentCount);
            
            long chunkCount = chunkRepository.count();
            log.info("[DiagnosticController] Chunk count query successful: {}", chunkCount);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Database is accessible",
                "documentCount", documentCount,
                "chunkCount", chunkCount
            ));
        } catch (Exception e) {
            log.error("[DiagnosticController] Database test failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Database connection failed: " + e.getMessage(),
                "errorType", e.getClass().getSimpleName(),
                "rootCause", e.getCause() != null ? e.getCause().getMessage() : "No root cause"
            ));
        }
    }
    
    @GetMapping("/embedding")
    public ResponseEntity<Map<String, Object>> testEmbedding() {
        log.info("[DiagnosticController] Testing embedding service");
        
        try {
            float[] embedding = embeddingService.generateQueryEmbedding("test query");
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Embedding service is working",
                "dimensions", embedding.length,
                "sampleValues", new float[]{embedding[0], embedding[1], embedding[2]}
            ));
        } catch (Exception e) {
            log.error("[DiagnosticController] Embedding test failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Embedding service failed: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/chat")
    public ResponseEntity<Map<String, Object>> testChat() {
        log.info("[DiagnosticController] Testing chat service");
        
        try {
            String response = resilientNvidiaChatClient.generateResponse(
                "You are a helpful assistant.", 
                "Say hello in one sentence."
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Chat service is working",
                "response", response,
                "responseLength", response.length()
            ));
        } catch (Exception e) {
            log.error("[DiagnosticController] Chat test failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Chat service failed: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/chat-flow")
    public ResponseEntity<Map<String, Object>> testChatFlow() {
        log.info("[DiagnosticController] Testing complete chat flow");
        
        try {
            String testQuery = "hello";
            
            // Step 1: Generate embedding
            log.info("[DiagnosticController] Step 1: Generating embedding");
            float[] embedding = embeddingService.generateQueryEmbedding(testQuery);
            log.info("[DiagnosticController] Step 1: SUCCESS - embedding dimensions: {}", embedding.length);
            
            // Step 2: Test database query
            log.info("[DiagnosticController] Step 2: Testing database query");
            String embeddingString = convertEmbeddingToString(embedding);
            java.util.List<Object[]> results = chunkRepository.findSimilarChunks(embeddingString, 0.25, 8);
            log.info("[DiagnosticController] Step 2: SUCCESS - found {} chunks", results.size());
            
            // Step 3: Test chat generation (only if we have chunks)
            String chatResponse = "No chunks found - would return default message";
            if (!results.isEmpty()) {
                log.info("[DiagnosticController] Step 3: Testing chat generation");
                chatResponse = resilientNvidiaChatClient.generateResponse(
                    "You are a helpful assistant.", 
                    testQuery
                );
                log.info("[DiagnosticController] Step 3: SUCCESS - response length: {}", chatResponse.length());
            } else {
                log.info("[DiagnosticController] Step 3: SKIPPED - no chunks to process");
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Complete chat flow test passed",
                "embeddingDimensions", embedding.length,
                "chunksFound", results.size(),
                "chatResponse", chatResponse
            ));
            
        } catch (Exception e) {
            log.error("[DiagnosticController] Chat flow test failed at some step", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Chat flow test failed: " + e.getMessage(),
                "errorType", e.getClass().getSimpleName()
            ));
        }
    }
    
    @GetMapping("/simple-chat")
    public ResponseEntity<Map<String, Object>> testSimpleChat() {
        log.info("[DiagnosticController] Testing simple chat without AppConfig");
        
        try {
            String testQuery = "hello";
            
            // Step 1: Generate embedding
            float[] embedding = embeddingService.generateQueryEmbedding(testQuery);
            
            // Step 2: Test database query with hardcoded values
            String embeddingString = convertEmbeddingToString(embedding);
            java.util.List<Object[]> results = chunkRepository.findSimilarChunks(embeddingString, 0.25, 8);
            
            // Step 3: Return results without calling chat API
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Simple chat test passed",
                "embeddingDimensions", embedding.length,
                "chunksFound", results.size(),
                "shouldReturnDefaultMessage", results.isEmpty()
            ));
            
        } catch (Exception e) {
            log.error("[DiagnosticController] Simple chat test failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Simple chat test failed: " + e.getMessage(),
                "errorType", e.getClass().getSimpleName()
            ));
        }
    }
    
    private String convertEmbeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}