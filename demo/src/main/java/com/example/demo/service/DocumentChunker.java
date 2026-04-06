package com.example.demo.service;

import com.example.demo.model.Document;
import com.example.demo.model.DocumentChunk;

import java.util.List;

/**
 * Service interface for splitting document text into overlapping chunks.
 * Implements intelligent chunking that preserves sentence boundaries and maintains
 * document context through overlapping tokens.
 * 
 * Validates Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
public interface DocumentChunker {
    
    /**
     * Split document text into overlapping chunks with sentence boundary preservation.
     * 
     * Creates chunks based on token count (configured via AppConfig) with overlap
     * to maintain context between chunks. Preserves sentence boundaries to avoid
     * splitting mid-sentence. Each chunk is assigned a sequential number and
     * associated with the parent document.
     * 
     * The chunks are created as DocumentChunk entities but NOT persisted to the
     * database. Persistence happens after embedding generation.
     * 
     * @param text The full document text to chunk
     * @param document The parent Document entity
     * @return List of DocumentChunk entities with content and metadata (no embeddings yet)
     * @throws IllegalArgumentException if text is null or empty
     */
    List<DocumentChunk> chunkDocument(String text, Document document);
    
    /**
     * Split text into overlapping chunks as strings.
     * 
     * @param text The text to chunk
     * @return List of text chunks
     * @throws IllegalArgumentException if text is null or empty
     */
    List<String> chunkText(String text);
}
