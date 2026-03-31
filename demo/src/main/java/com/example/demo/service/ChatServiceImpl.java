package com.example.demo.service;

import com.example.demo.config.AppConfig;
import com.example.demo.dto.ChatResponse;
import com.example.demo.dto.RetrievedChunk;
import com.example.demo.dto.SourceReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ChatService that orchestrates RAG query processing.
 * Handles embedding generation, retrieval, context construction, and LLM response generation.
 * 
 * Validates Requirements 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final ResilientNvidiaChatClient resilientNvidiaChatClient;
    private final AppConfig appConfig;
    
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a helpful assistant that answers questions based solely on the provided context.
            
            IMPORTANT INSTRUCTIONS:
            - Answer ONLY using information from the context below
            - If the context does not contain enough information to answer the question, say: "I don't have enough information in the uploaded documents to answer that question"
            - Do not use external knowledge or make assumptions
            - Be concise and accurate
            - Cite specific parts of the context when relevant
            
            CONTEXT:
            {context}
            """;
    
    private static final String NO_CONTEXT_RESPONSE = 
        "I don't have enough information in the uploaded documents to answer that question";
    
    @Override
    public ChatResponse processQuery(String query) {
        String queryPreview = query.length() > 100 ? query.substring(0, 100) + "..." : query;
        log.info("[ChatService] Processing query - queryLength: {}, queryPreview: '{}', timestamp: {}", 
                query.length(), queryPreview, java.time.LocalDateTime.now());
        
        // Step 1: Sanitize user input to prevent prompt injection
        log.debug("[ChatService] Sanitizing user input - originalLength: {}", query.length());
        String sanitizedQuery = sanitizeInput(query);
        log.debug("[ChatService] Input sanitized - sanitizedLength: {}", sanitizedQuery.length());
        
        // Step 2: Generate query embedding
        log.debug("[ChatService] Generating query embedding");
        float[] queryEmbedding = embeddingService.generateQueryEmbedding(sanitizedQuery);
        log.debug("[ChatService] Query embedding generated - dimensions: {}", queryEmbedding.length);
        
        // Step 3: Retrieve relevant chunks
        int topK = appConfig.getRetrieval().getTopK();
        double threshold = appConfig.getRetrieval().getSimilarityThreshold();
        log.debug("[ChatService] Retrieving similar chunks - topK: {}, threshold: {}", topK, threshold);
        
        List<RetrievedChunk> retrievedChunks = retrievalService.retrieveSimilarChunks(
            queryEmbedding, topK, threshold
        );
        
        log.info("[ChatService] Retrieved chunks - count: {}", retrievedChunks.size());
        
        // Step 4: Handle case when no relevant chunks found
        if (retrievedChunks.isEmpty()) {
            log.info("[ChatService] No relevant chunks found - returning default response, timestamp: {}", 
                    java.time.LocalDateTime.now());
            return ChatResponse.builder()
                .answer(NO_CONTEXT_RESPONSE)
                .sources(List.of())
                .retrievedChunks(0)
                .build();
        }
        
        // Step 5: Construct context window from retrieved chunks
        log.debug("[ChatService] Constructing context window - chunkCount: {}", retrievedChunks.size());
        String contextWindow = constructContextWindow(retrievedChunks);
        log.debug("[ChatService] Context window constructed - contextLength: {} chars", contextWindow.length());
        
        // Step 6: Build system prompt and call NVIDIA LLM API
        log.debug("[ChatService] Generating response from NVIDIA LLM API");
        String answer = generateResponseWithRetry(contextWindow, sanitizedQuery);
        log.debug("[ChatService] Response generated - answerLength: {} chars", answer.length());
        
        // Step 7: Include source references in response
        List<SourceReference> sources = buildSourceReferences(retrievedChunks);
        log.debug("[ChatService] Built source references - sourceCount: {}", sources.size());
        
        log.info("[ChatService] Query processing completed - retrievedChunks: {}, sources: {}, answerLength: {}, timestamp: {}", 
                retrievedChunks.size(), sources.size(), answer.length(), java.time.LocalDateTime.now());
        
        return ChatResponse.builder()
            .answer(answer)
            .sources(sources)
            .retrievedChunks(retrievedChunks.size())
            .build();
    }
    
    /**
     * Sanitize user input to prevent prompt injection attacks.
     * Removes or escapes potentially malicious patterns.
     * 
     * @param input Raw user input
     * @return Sanitized input
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            log.debug("[ChatService] Null input received, returning empty string");
            return "";
        }
        
        log.trace("[ChatService] Sanitizing input - originalLength: {}", input.length());
        
        // Remove null bytes
        String sanitized = input.replace("\0", "");
        
        // Remove control characters except newlines and tabs
        sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        
        // Trim excessive whitespace
        sanitized = sanitized.trim();
        
        // Limit length (already validated by @Size annotation, but double-check)
        if (sanitized.length() > 1000) {
            log.debug("[ChatService] Input truncated - originalLength: {}, truncatedTo: 1000", sanitized.length());
            sanitized = sanitized.substring(0, 1000);
        }
        
        log.trace("[ChatService] Input sanitization completed - sanitizedLength: {}", sanitized.length());
        return sanitized;
    }
    
    /**
     * Construct context window from retrieved chunks.
     * Formats chunks with clear separation and metadata.
     * 
     * @param chunks Retrieved chunks ordered by similarity
     * @return Formatted context string
     */
    private String constructContextWindow(List<RetrievedChunk> chunks) {
        log.trace("[ChatService] Constructing context window - chunkCount: {}", chunks.size());
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            context.append(String.format("[Document: %s, Chunk %d]\n", 
                chunk.getFilename(), chunk.getChunkNumber()));
            context.append(chunk.getContent());
            
            if (i < chunks.size() - 1) {
                context.append("\n\n---\n\n");
            }
        }
        
        log.trace("[ChatService] Context window construction completed - totalLength: {}", context.length());
        return context.toString();
    }
    
    /**
     * Generate response from NVIDIA LLM API with retry logic.
     * Now uses resilient client with circuit breaker protection.
     * 
     * @param context Context window from retrieved chunks
     * @param query User's sanitized query
     * @return Generated answer
     */
    private String generateResponseWithRetry(String context, String query) {
        log.debug("[ChatService] Calling resilient NVIDIA LLM API");
        
        // Build system prompt with context
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.replace("{context}", context);
        
        // Call resilient client (circuit breaker, retry, rate limiter applied)
        return resilientNvidiaChatClient.generateResponse(systemPrompt, query);
    }
    
    /**
     * Build source references from retrieved chunks.
     * 
     * @param chunks Retrieved chunks
     * @return List of source references
     */
    private List<SourceReference> buildSourceReferences(List<RetrievedChunk> chunks) {
        log.trace("[ChatService] Building source references - chunkCount: {}", chunks.size());
        
        return chunks.stream()
            .map(chunk -> {
                log.trace("[ChatService] Creating source reference - documentId: {}, filename: {}, chunkNumber: {}, score: {}", 
                         chunk.getDocumentId(), chunk.getFilename(), chunk.getChunkNumber(), chunk.getSimilarityScore());
                return SourceReference.builder()
                    .documentId(chunk.getDocumentId())
                    .filename(chunk.getFilename())
                    .chunkNumber(chunk.getChunkNumber())
                    .similarityScore(chunk.getSimilarityScore())
                    .build();
            })
            .collect(Collectors.toList());
    }
}
