package com.example.demo.service;

import com.example.demo.model.DocumentChunk;

import java.util.List;

/**
 * Service interface for generating vector embeddings using Google Gemini API.
 * Handles both batch embedding generation for document chunks and single query embeddings.
 * 
 * Validates Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
public interface EmbeddingService {
    
    /**
     * Generate embeddings for document chunks in batches.
     * Processes chunks in configurable batch sizes to optimize API usage.
     * Implements retry logic with exponential backoff for resilience.
     * 
     * @param chunks List of chunks to embed
     * @return List of chunks with embeddings populated
     * @throws RuntimeException if all retry attempts fail
     */
    List<DocumentChunk> generateEmbeddings(List<DocumentChunk> chunks);
    
    /**
     * Generate embedding for a single query string.
     * Used for semantic search to find similar document chunks.
     * 
     * @param query The query text
     * @return Embedding vector as float array
     * @throws RuntimeException if embedding generation fails after retries
     */
    float[] generateQueryEmbedding(String query);
}
