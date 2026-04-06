package com.example.demo.repository;

import com.example.demo.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for DocumentChunk entity providing CRUD operations
 * and vector similarity search capabilities using PostgreSQL PGVector.
 * 
 * Validates Requirements 4.2, 5.2, 5.3, 5.5
 */
@Repository
public interface ChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    /**
     * Find all chunks belonging to a specific document.
     * 
     * @param documentId The document ID
     * @return List of chunks for the document
     */
    List<DocumentChunk> findByDocumentId(Long documentId);
    
    /**
     * Delete all chunks belonging to a specific document.
     * 
     * @param documentId The document ID
     * @return Number of deleted chunks
     */
    int deleteByDocumentId(Long documentId);
    
    /**
     * Perform vector similarity search using cosine distance.
     * Returns the top-k most similar chunks with similarity scores above the threshold.
     * Includes document metadata (filename) via JOIN.
     * 
     * Optimized query with:
     * - Threshold filter applied before sorting for faster execution
     * - Minimal column selection to reduce data transfer
     * 
     * The similarity score is calculated as (1 - cosine_distance), where:
     * - 1.0 means identical vectors
     * - 0.0 means orthogonal vectors
     * 
     * @param queryEmbedding The query embedding vector
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param topK Maximum number of results to return
     * @return List of chunks with document metadata ordered by descending similarity score
     */
    @Query(value = """
        SELECT c.id, c.document_id, c.chunk_number, c.content, c.token_count, 
               NULL as embedding, c.created_at, d.filename,
               (1 - (c.embedding <=> CAST(:queryEmbedding AS vector))) AS similarity_score
        FROM document_chunks c
        INNER JOIN documents d ON c.document_id = d.id
        WHERE (1 - (c.embedding <=> CAST(:queryEmbedding AS vector))) >= :similarityThreshold
        ORDER BY c.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> findSimilarChunks(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("similarityThreshold") double similarityThreshold,
        @Param("topK") int topK
    );
}
