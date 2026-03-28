package com.example.demo.service;

import com.example.demo.dto.RetrievedChunk;

import java.util.List;

/**
 * Service interface for retrieving similar document chunks based on vector similarity.
 * Performs semantic search using embeddings stored in the vector database.
 * 
 * Validates Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
public interface RetrievalService {
    
    /**
     * Retrieve top-k similar chunks for a query embedding.
     * Performs cosine similarity search and filters results by threshold.
     * 
     * @param queryEmbedding The query embedding vector
     * @param topK Number of results to return (default: 5)
     * @param similarityThreshold Minimum similarity score (default: 0.7)
     * @return List of relevant chunks ordered by descending similarity score
     */
    List<RetrievedChunk> retrieveSimilarChunks(
        float[] queryEmbedding, 
        int topK, 
        double similarityThreshold
    );
}
