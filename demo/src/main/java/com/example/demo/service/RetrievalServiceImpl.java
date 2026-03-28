package com.example.demo.service;

import com.example.demo.dto.RetrievedChunk;
import com.example.demo.repository.ChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of RetrievalService that performs semantic search using vector similarity.
 * Queries the ChunkRepository with cosine similarity and maps results to RetrievedChunk DTOs.
 * 
 * Validates Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalServiceImpl implements RetrievalService {
    
    private final ChunkRepository chunkRepository;
    
    /**
     * Retrieve top-k similar chunks for a query embedding.
     * Converts the float[] embedding to PostgreSQL vector format string,
     * queries the database, and maps Object[] results to RetrievedChunk DTOs.
     * 
     * @param queryEmbedding The query embedding vector
     * @param topK Number of results to return
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @return List of relevant chunks ordered by descending similarity score, or empty list if no chunks meet threshold
     */
    @Override
    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieveSimilarChunks(
            float[] queryEmbedding, 
            int topK, 
            double similarityThreshold) {
        
        log.info("[RetrievalService] Starting similarity search - topK: {}, threshold: {}, embeddingDimensions: {}, timestamp: {}", 
                topK, similarityThreshold, queryEmbedding.length, java.time.LocalDateTime.now());
        
        // Convert float[] to PostgreSQL vector format string: "[0.1,0.2,0.3,...]"
        log.debug("[RetrievalService] Converting embedding to vector format - dimensions: {}", queryEmbedding.length);
        String embeddingString = convertEmbeddingToString(queryEmbedding);
        
        // Query the repository
        log.debug("[RetrievalService] Executing vector similarity query - topK: {}, threshold: {}", topK, similarityThreshold);
        List<Object[]> results = chunkRepository.findSimilarChunks(
            embeddingString, 
            similarityThreshold, 
            topK
        );
        
        // If no results meet the threshold, return empty list
        if (results.isEmpty()) {
            log.info("[RetrievalService] No chunks found meeting similarity threshold - threshold: {}, timestamp: {}", 
                    similarityThreshold, java.time.LocalDateTime.now());
            return Collections.emptyList();
        }
        
        log.debug("[RetrievalService] Raw results retrieved - resultCount: {}", results.size());
        
        // Map Object[] results to RetrievedChunk DTOs
        List<RetrievedChunk> retrievedChunks = results.stream()
            .map(this::mapToRetrievedChunk)
            .collect(Collectors.toList());
        
        // Log summary of retrieved chunks
        if (!retrievedChunks.isEmpty()) {
            double maxScore = retrievedChunks.get(0).getSimilarityScore();
            double minScore = retrievedChunks.get(retrievedChunks.size() - 1).getSimilarityScore();
            log.info("[RetrievalService] Similarity search completed - retrievedChunks: {}, maxScore: {}, minScore: {}, timestamp: {}", 
                    retrievedChunks.size(), maxScore, minScore, java.time.LocalDateTime.now());
        }
        
        return retrievedChunks;
    }
    
    /**
     * Convert float array embedding to PostgreSQL vector format string.
     * Format: "[0.1,0.2,0.3,...]"
     * 
     * @param embedding The embedding vector
     * @return String representation in PostgreSQL vector format
     */
    private String convertEmbeddingToString(float[] embedding) {
        log.trace("[RetrievalService] Converting embedding array to string - dimensions: {}", embedding.length);
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
    
    /**
     * Map Object[] result from native query to RetrievedChunk DTO.
     * 
     * Query result columns:
     * [0] id (Long)
     * [1] document_id (Long)
     * [2] chunk_number (Integer)
     * [3] content (String)
     * [4] token_count (Integer)
     * [5] embedding (not used in DTO)
     * [6] created_at (not used in DTO)
     * [7] filename (String)
     * [8] similarity_score (Double)
     * 
     * @param row The result row from native query
     * @return RetrievedChunk DTO with document metadata
     */
    private RetrievedChunk mapToRetrievedChunk(Object[] row) {
        Long chunkId = ((Number) row[0]).longValue();
        Long documentId = ((Number) row[1]).longValue();
        Integer chunkNumber = (Integer) row[2];
        String content = (String) row[3];
        String filename = (String) row[7];
        Double similarityScore = (Double) row[8];
        
        log.trace("[RetrievalService] Mapping chunk - chunkId: {}, documentId: {}, chunkNumber: {}, filename: {}, similarityScore: {}", 
                 chunkId, documentId, chunkNumber, filename, similarityScore);
        
        return RetrievedChunk.builder()
            .chunkId(chunkId)
            .documentId(documentId)
            .filename(filename)
            .chunkNumber(chunkNumber)
            .content(content)
            .similarityScore(similarityScore)
            .build();
    }
}
