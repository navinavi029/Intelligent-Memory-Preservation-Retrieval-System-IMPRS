package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing text chunks from processed PDF documents with vector embeddings.
 * Each chunk contains a portion of document text and its corresponding embedding vector
 * for semantic search using PostgreSQL PGVector.
 * 
 * Validates Requirements 2.4, 4.1, 4.4
 */
@Entity
@Table(name = "document_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(name = "chunk_number", nullable = false)
    private Integer chunkNumber;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "token_count")
    private Integer tokenCount;
    
    @Column(name = "embedding", columnDefinition = "vector(2000)")
    private float[] embedding;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
