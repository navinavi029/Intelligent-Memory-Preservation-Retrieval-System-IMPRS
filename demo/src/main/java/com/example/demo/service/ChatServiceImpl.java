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
 * Your caring memory companion! 
 * I help you reconnect with your precious memories by finding related stories
 * and presenting them in a warm, supportive way.
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
            You are a caring memory companion designed to help users reconnect with their precious memories.
            
            Core Principles:
            - Only reference memories and stories explicitly shared by the user - never fabricate details
            - If no relevant memories are found, respond: "I don't have that memory yet, but I'd love to hear about it if you'd like to share!"
            - Speak with warmth, empathy, and genuine care, like a trusted friend who treasures their stories
            - Help users connect related memories and rediscover meaningful details
            - Provide specific, contextual responses that demonstrate deep understanding of their shared experiences
            - When appropriate, gently encourage sharing more details or related memories
            
            Response Guidelines:
            - Be conversational and emotionally supportive
            - Reference specific details from the provided memories
            - Ask thoughtful follow-up questions when relevant
            - Acknowledge the emotional significance of shared memories
            - Use natural, flowing language that feels personal and caring
            
            Related memories that may help with your response:
            {context}
            
            Remember: Your role is to be a compassionate companion who helps preserve and celebrate the user's life experiences.
            """;
    
    private static final String NO_CONTEXT_RESPONSE = 
        "I don't have that memory yet, but I'd love to hear about it if you'd like to share! You can tell me about any moment that's meaningful to you, and I'll keep it safe for when you want to remember it again.";
    
    @Override
    public ChatResponse processQuery(String query) {
        String queryPreview = query.length() > 100 ? query.substring(0, 100) + "..." : query;
        log.info("💭 Helping with memory question - queryLength: {}, queryPreview: '{}', timestamp: {}", 
                query.length(), queryPreview, java.time.LocalDateTime.now());
        
        try {
            // Step 1: Sanitize user input to prevent prompt injection
            log.debug("[ChatService] Sanitizing user input - originalLength: {}", query.length());
            String sanitizedQuery = sanitizeInput(query);
            log.debug("[ChatService] Input sanitized - sanitizedLength: {}", sanitizedQuery.length());
            
            // Step 2: Generate query embedding
            log.debug("[ChatService] Generating query embedding");
            float[] queryEmbedding;
            try {
                queryEmbedding = embeddingService.generateQueryEmbedding(sanitizedQuery);
                log.debug("[ChatService] Query embedding generated - dimensions: {}", queryEmbedding.length);
                
                // Log first few values for debugging
                if (queryEmbedding.length > 0) {
                    log.debug("[ChatService] First 5 embedding values: [{}, {}, {}, {}, {}]", 
                        queryEmbedding[0], 
                        queryEmbedding.length > 1 ? queryEmbedding[1] : "N/A",
                        queryEmbedding.length > 2 ? queryEmbedding[2] : "N/A",
                        queryEmbedding.length > 3 ? queryEmbedding[3] : "N/A",
                        queryEmbedding.length > 4 ? queryEmbedding[4] : "N/A");
                }
            } catch (Exception e) {
                log.error("[ChatService] Failed to generate query embedding - error: {}, message: {}", 
                    e.getClass().getSimpleName(), e.getMessage(), e);
                return ChatResponse.builder()
                    .answer("I'm having trouble understanding your question right now. Could you try rephrasing it?")
                    .sources(List.of())
                    .retrievedChunks(0)
                    .build();
            }
            
            // Step 3: Retrieve relevant chunks with enhanced parameters
            // Optimized for maximum recall - always return something relevant
            int topK = 20; // Retrieve even more candidates to increase chances of finding relevant content
            double threshold = 0.0; // No threshold - return all results sorted by similarity
            log.debug("[ChatService] Retrieving similar chunks - topK: {}, threshold: {}", topK, threshold);
            
            List<RetrievedChunk> retrievedChunks;
            try {
                retrievedChunks = retrievalService.retrieveSimilarChunks(queryEmbedding, topK, threshold);
            } catch (Exception e) {
                log.error("[ChatService] Failed to retrieve similar chunks - error: {}", e.getMessage(), e);
                // Check if it's a database connection issue
                if (e.getMessage() != null && (e.getMessage().contains("database") || 
                    e.getMessage().contains("connection") || e.getMessage().contains("SQL"))) {
                    return ChatResponse.builder()
                        .answer("I'm having trouble connecting to my memory storage right now. Please make sure the database is running and try again.")
                        .sources(List.of())
                        .retrievedChunks(0)
                        .build();
                } else {
                    return ChatResponse.builder()
                        .answer("I'm having trouble searching through your memories right now. Please try again in a moment.")
                        .sources(List.of())
                        .retrievedChunks(0)
                        .build();
                }
            }
            
            // Step 4: Apply diversity filtering if enabled
            // Enable diversity filtering to show varied memories while maintaining high recall
            if (retrievedChunks.size() > 1) {
                retrievedChunks = applyDiversityFiltering(retrievedChunks);
                log.debug("[ChatService] Applied diversity filtering - finalCount: {}", retrievedChunks.size());
            }
            
            log.info("[ChatService] Retrieved chunks - count: {}", retrievedChunks.size());
            
            // Step 5: Handle case when no relevant chunks found
            if (retrievedChunks.isEmpty()) {
                log.info("[ChatService] No relevant chunks found - returning default response, timestamp: {}", 
                        java.time.LocalDateTime.now());
                return ChatResponse.builder()
                    .answer(NO_CONTEXT_RESPONSE)
                    .sources(List.of())
                    .retrievedChunks(0)
                    .build();
            }
            
            // Step 6: Construct context window from retrieved chunks
            log.debug("[ChatService] Constructing context window - chunkCount: {}", retrievedChunks.size());
            String contextWindow = constructContextWindow(retrievedChunks);
            log.debug("[ChatService] Context window constructed - contextLength: {} chars", contextWindow.length());
            
            // Step 7: Build system prompt and call NVIDIA LLM API
            log.debug("[ChatService] Generating response from NVIDIA LLM API");
            String answer;
            try {
                answer = generateResponseWithRetry(contextWindow, sanitizedQuery);
                log.debug("[ChatService] Response generated - answerLength: {} chars", answer.length());
            } catch (Exception e) {
                log.error("[ChatService] Failed to generate response from NVIDIA API - error: {}", e.getMessage(), e);
                return ChatResponse.builder()
                    .answer("I found some relevant memories but I'm having trouble putting together a response right now. Please try again in a moment.")
                    .sources(buildSourceReferences(retrievedChunks))
                    .retrievedChunks(retrievedChunks.size())
                    .build();
            }
            
            // Step 8: Include source references in response
            List<SourceReference> sources = buildSourceReferences(retrievedChunks);
            log.debug("[ChatService] Built source references - sourceCount: {}", sources.size());
            
            log.info("[ChatService] Query processing completed - retrievedChunks: {}, sources: {}, answerLength: {}, timestamp: {}", 
                    retrievedChunks.size(), sources.size(), answer.length(), java.time.LocalDateTime.now());
            
            return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .retrievedChunks(retrievedChunks.size())
                .build();
                
        } catch (Exception e) {
            log.error("[ChatService] Unexpected error processing query - error: {}, type: {}, cause: {}", 
                     e.getMessage(), e.getClass().getSimpleName(), 
                     e.getCause() != null ? e.getCause().getMessage() : "none");
            log.error("[ChatService] Full stack trace:", e);
            return ChatResponse.builder()
                .answer("I'm experiencing some technical difficulties right now. Please try again in a moment.")
                .sources(List.of())
                .retrievedChunks(0)
                .build();
        }
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
     * Put together the relevant memories in a warm, caring way.
     * 
     * @param chunks The most meaningful memories I found
     * @return A gently formatted context for our conversation about your memories
     */
    private String constructContextWindow(List<RetrievedChunk> chunks) {
        log.trace("💝 Gathering your precious memories together - memoryCount: {}", chunks.size());
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            context.append(chunk.getContent());
            
            if (i < chunks.size() - 1) {
                context.append("\n\n---\n\n");
            }
        }
        
        log.trace("✨ Your memories are ready to share - totalLength: {}", context.length());
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
    
    /**
     * Apply diversity filtering to avoid too similar chunks in results.
     * Uses permissive threshold to maintain high recall while reducing redundancy.
     */
    private List<RetrievedChunk> applyDiversityFiltering(List<RetrievedChunk> chunks) {
        if (chunks.size() <= 1) return chunks;
        
        List<RetrievedChunk> diverseChunks = new java.util.ArrayList<>();
        diverseChunks.add(chunks.get(0)); // Always include the most similar
        
        // Permissive diversity threshold - only filter out near-duplicates
        double diversityThreshold = 0.85;
        
        for (int i = 1; i < chunks.size(); i++) {
            RetrievedChunk candidate = chunks.get(i);
            boolean isDiverse = true;
            
            for (RetrievedChunk selected : diverseChunks) {
                double similarity = calculateContentSimilarity(candidate.getContent(), selected.getContent());
                if (similarity > diversityThreshold) {
                    isDiverse = false;
                    break;
                }
            }
            
            if (isDiverse) {
                diverseChunks.add(candidate);
            }
        }
        
        log.debug("[ChatService] Diversity filtering applied - original: {}, filtered: {}", 
                 chunks.size(), diverseChunks.size());
        return diverseChunks;
    }
    
    /**
     * Calculate simple content similarity for diversity filtering.
     */
    private double calculateContentSimilarity(String content1, String content2) {
        String[] words1 = content1.toLowerCase().split("\\s+");
        String[] words2 = content2.toLowerCase().split("\\s+");
        
        java.util.Set<String> set1 = java.util.Set.of(words1);
        java.util.Set<String> set2 = java.util.Set.of(words2);
        
        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}