package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO representing a chunk retrieved from the vector store with its similarity score.
 * Used internally by the retrieval service to pass chunk data with metadata to the chat service.
 * 
 * Validates Requirements 5.6, 7.2
 */
@Data
@Builder
public class RetrievedChunk {
    
    /**
     * Unique identifier for the chunk
     */
    private Long chunkId;
    
    /**
     * ID of the parent document
     */
    private Long documentId;
    
    /**
     * Original filename of the source document
     */
    private String filename;
    
    /**
     * Sequential chunk number within the document
     */
    private Integer chunkNumber;
    
    /**
     * Text content of the chunk
     */
    private String content;
    
    /**
     * Cosine similarity score between query and chunk (0.0 to 1.0)
     */
    private Double similarityScore;
}
