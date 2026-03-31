package com.example.demo.service;

import com.example.demo.config.AppConfig;
import com.example.demo.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of EmbeddingService using NVIDIA NIM embedding model.
 * Provides batch processing and retry logic with exponential backoff for resilience.
 * 
 * Validates Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 5.1, 9.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {
    
    private final NvidiaEmbeddingClient nvidiaClient;
    private final AppConfig appConfig;
    
    @Override
    public List<DocumentChunk> generateEmbeddings(List<DocumentChunk> chunks) {
        log.info("[EmbeddingService] Starting embedding generation - chunkCount: {}, timestamp: {}", 
                chunks.size(), LocalDateTime.now());
        
        int batchSize = appConfig.getEmbedding().getBatchSize();
        List<DocumentChunk> processedChunks = new ArrayList<>();
        
        int totalBatches = (chunks.size() + batchSize - 1) / batchSize;
        log.debug("[EmbeddingService] Batch processing configuration - totalChunks: {}, batchSize: {}, totalBatches: {}", 
                 chunks.size(), batchSize, totalBatches);
        
        // Process chunks in batches
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunks.size());
            List<DocumentChunk> batch = chunks.subList(i, endIndex);
            int batchNumber = (i / batchSize) + 1;
            
            log.debug("[EmbeddingService] Processing batch - batchNumber: {}/{}, chunkRange: {}-{}, batchSize: {}", 
                     batchNumber, totalBatches, i, endIndex - 1, batch.size());
            
            List<DocumentChunk> processedBatch = processBatchWithRetry(batch);
            processedChunks.addAll(processedBatch);
            
            log.debug("[EmbeddingService] Batch completed - batchNumber: {}/{}, processedChunks: {}", 
                     batchNumber, totalBatches, processedBatch.size());
        }
        
        log.info("[EmbeddingService] Embedding generation completed - totalChunks: {}, successfulChunks: {}, timestamp: {}", 
                chunks.size(), processedChunks.size(), LocalDateTime.now());
        return processedChunks;
    }
    
    @Override
    @Cacheable(value = "queryEmbeddings", key = "#query")
    public float[] generateQueryEmbedding(String query) {
        String queryPreview = query.length() > 50 ? query.substring(0, 50) + "..." : query;
        log.debug("[EmbeddingService] Generating query embedding - queryLength: {}, queryPreview: '{}', timestamp: {}", 
                 query.length(), queryPreview, LocalDateTime.now());
        
        float[] embedding = executeWithRetry(() -> {
            List<float[]> embeddings = nvidiaClient.generateEmbeddings(List.of(query));
            
            if (embeddings.isEmpty()) {
                log.error("[EmbeddingService] Empty embedding response - component: EmbeddingService, queryLength: {}, timestamp: {}", 
                         query.length(), LocalDateTime.now());
                throw new RuntimeException("Empty embedding response for query");
            }
            
            return embeddings.get(0);
        }, "query embedding");
        
        log.debug("[EmbeddingService] Query embedding generated - dimensions: {}, timestamp: {}", 
                 embedding.length, LocalDateTime.now());
        return embedding;
    }
    
    /**
     * Process a batch of chunks with retry logic.
     */
    private List<DocumentChunk> processBatchWithRetry(List<DocumentChunk> batch) {
        log.debug("[EmbeddingService] Starting batch embedding - batchSize: {}", batch.size());
        
        return executeWithRetry(() -> {
            // Extract text content from chunks
            List<String> texts = batch.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.toList());
            
            log.debug("[EmbeddingService] Calling NVIDIA API - batchSize: {}, totalChars: {}", 
                     texts.size(), texts.stream().mapToInt(String::length).sum());
            
            // Generate embeddings using custom NVIDIA client
            List<float[]> embeddings = nvidiaClient.generateEmbeddings(texts);
            
            if (embeddings.size() != batch.size()) {
                log.error("[EmbeddingService] Embedding count mismatch - component: EmbeddingService, expected: {}, received: {}, timestamp: {}", 
                         batch.size(), embeddings.size(), LocalDateTime.now());
                throw new RuntimeException(
                    String.format("Embedding count mismatch: expected %d, got %d",
                                batch.size(), embeddings.size())
                );
            }
            
            // Assign embeddings to chunks
            for (int i = 0; i < batch.size(); i++) {
                float[] embedding = embeddings.get(i);
                batch.get(i).setEmbedding(embedding);
                log.trace("[EmbeddingService] Embedding assigned - chunkIndex: {}, dimensions: {}", i, embedding.length);
            }
            
            log.debug("[EmbeddingService] Batch embedding completed - batchSize: {}", batch.size());
            return batch;
        }, "batch embedding");
    }
    
    /**
     * Execute an operation with exponential backoff retry logic.
     * Logs API rate limit warnings and implements exponential backoff (Requirement 9.3).
     * 
     * @param operation The operation to execute
     * @param operationType Description of the operation for logging
     * @return Result of the operation
     * @throws RuntimeException if all retry attempts fail
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationType) {
        int maxAttempts = appConfig.getRetry().getMaxAttempts();
        long initialDelay = appConfig.getRetry().getInitialDelay();
        double multiplier = appConfig.getRetry().getMultiplier();
        
        log.debug("[EmbeddingService] Starting retry operation - operationType: {}, maxAttempts: {}, initialDelay: {}ms, multiplier: {}", 
                 operationType, maxAttempts, initialDelay, multiplier);
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.trace("[EmbeddingService] Executing attempt - operationType: {}, attempt: {}/{}", 
                         operationType, attempt, maxAttempts);
                T result = operation.execute();
                
                if (attempt > 1) {
                    log.info("[EmbeddingService] Operation succeeded after retry - operationType: {}, successfulAttempt: {}/{}, timestamp: {}", 
                            operationType, attempt, maxAttempts, LocalDateTime.now());
                }
                
                return result;
            } catch (Exception e) {
                lastException = e;
                
                // Check if this is a rate limit error (Requirement 9.3)
                boolean isRateLimitError = e.getMessage() != null && 
                    (e.getMessage().contains("rate limit") || 
                     e.getMessage().contains("429") ||
                     e.getMessage().contains("quota"));
                
                if (attempt < maxAttempts) {
                    long delay = (long) (initialDelay * Math.pow(multiplier, attempt - 1));
                    
                    if (isRateLimitError) {
                        log.warn("[EmbeddingService] API rate limit exceeded - component: EmbeddingService, operationType: {}, attempt: {}/{}, retryDelay: {}ms, timestamp: {}, error: {}", 
                                operationType, attempt, maxAttempts, delay, LocalDateTime.now(), e.getMessage());
                    } else {
                        log.warn("[EmbeddingService] Operation failed - component: EmbeddingService, operationType: {}, attempt: {}/{}, retryDelay: {}ms, timestamp: {}, error: {}", 
                                operationType, attempt, maxAttempts, delay, LocalDateTime.now(), e.getMessage());
                    }
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[EmbeddingService] Retry interrupted - component: EmbeddingService, operationType: {}, timestamp: {}", 
                                 operationType, LocalDateTime.now(), ie);
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    log.error("[EmbeddingService] Operation failed after all retries - component: EmbeddingService, operationType: {}, totalAttempts: {}, timestamp: {}, error: {}", 
                             operationType, maxAttempts, LocalDateTime.now(), e.getMessage(), e);
                }
            }
        }
        
        throw new RuntimeException(
            String.format("Failed to generate %s after %d attempts", operationType, maxAttempts),
            lastException
        );
    }
    
    /**
     * Functional interface for retryable operations.
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}
