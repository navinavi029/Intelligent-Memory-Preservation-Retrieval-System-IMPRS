package com.example.demo.service;

import com.example.demo.config.AppConfig;
import com.example.demo.model.Document;
import com.example.demo.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of DocumentChunker for splitting text into semantic chunks.
 * Uses simple whitespace tokenization and sentence boundary detection to create
 * overlapping chunks that preserve context and readability.
 * 
 * Validates Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentChunkerImpl implements DocumentChunker {
    
    private final AppConfig appConfig;
    
    // Pattern to detect sentence boundaries (. ! ? followed by space or end of string)
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[.!?](?=\\s|$)");
    
    /**
     * Split document text into overlapping chunks with sentence boundary preservation.
     * 
     * Algorithm:
     * 1. Tokenize text using whitespace splitting
     * 2. Create chunks of configured size with overlap
     * 3. Adjust chunk boundaries to preserve sentence endings
     * 4. Assign sequential chunk numbers
     * 5. Create DocumentChunk entities with metadata
     * 
     * @param text The full document text to chunk
     * @param document The parent Document entity
     * @return List of DocumentChunk entities (not yet persisted)
     */
    @Override
    public List<DocumentChunk> chunkDocument(String text, Document document) {
        if (text == null || text.trim().isEmpty()) {
            log.error("[DocumentChunker] Cannot chunk null or empty text - documentId: {}, component: DocumentChunker, timestamp: {}", 
                     document.getId(), LocalDateTime.now());
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        log.info("[DocumentChunker] Starting document chunking - documentId: {}, filename: {}, textLength: {} chars, timestamp: {}", 
                document.getId(), document.getFilename(), text.length(), LocalDateTime.now());
        
        int chunkSize = appConfig.getChunking().getChunkSize();
        int overlap = appConfig.getChunking().getOverlap();
        
        log.debug("[DocumentChunker] Chunking configuration - documentId: {}, chunkSize: {}, overlap: {}", 
                 document.getId(), chunkSize, overlap);
        
        // Tokenize text using whitespace
        String[] tokens = tokenize(text);
        log.debug("[DocumentChunker] Tokenized text - documentId: {}, tokenCount: {}", 
                 document.getId(), tokens.length);
        
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkNumber = 0;
        int currentPosition = 0;
        
        while (currentPosition < tokens.length) {
            // Determine end position for this chunk
            int endPosition = Math.min(currentPosition + chunkSize, tokens.length);
            
            // Extract chunk tokens
            String[] chunkTokens = new String[endPosition - currentPosition];
            System.arraycopy(tokens, currentPosition, chunkTokens, 0, chunkTokens.length);
            
            // Reconstruct text from tokens
            String chunkText = String.join(" ", chunkTokens);
            
            // Adjust chunk boundary to preserve sentence endings (Requirement 2.2, 2.3)
            if (endPosition < tokens.length) {
                chunkText = adjustToSentenceBoundary(chunkText, tokens, currentPosition, endPosition);
            }
            
            // Create DocumentChunk entity (Requirement 2.4, 2.5)
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .chunkNumber(chunkNumber)
                    .content(chunkText)
                    .tokenCount(countTokens(chunkText))
                    .createdAt(LocalDateTime.now())
                    .build();
            
            chunks.add(chunk);
            log.debug("[DocumentChunker] Created chunk - documentId: {}, chunkNumber: {}, tokenCount: {}, contentLength: {} chars", 
                     document.getId(), chunkNumber, chunk.getTokenCount(), chunkText.length());
            
            chunkNumber++;
            
            // Move to next chunk position with overlap (Requirement 2.1)
            currentPosition += (chunkSize - overlap);
            
            // Prevent infinite loop if overlap >= chunkSize
            if (currentPosition <= currentPosition - (chunkSize - overlap)) {
                currentPosition = endPosition;
            }
        }
        
        log.info("[DocumentChunker] Document chunking completed - documentId: {}, filename: {}, totalChunks: {}, timestamp: {}", 
                document.getId(), document.getFilename(), chunks.size(), LocalDateTime.now());
        
        return chunks;
    }
    
    @Override
    public List<String> chunkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.error("[DocumentChunker] Cannot chunk null or empty text - component: DocumentChunker, timestamp: {}", 
                     LocalDateTime.now());
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        log.info("[DocumentChunker] Starting text chunking - textLength: {} chars, timestamp: {}", 
                text.length(), LocalDateTime.now());
        
        int chunkSize = appConfig.getChunking().getChunkSize();
        int overlap = appConfig.getChunking().getOverlap();
        
        log.debug("[DocumentChunker] Chunking configuration - chunkSize: {}, overlap: {}", 
                 chunkSize, overlap);
        
        // Tokenize text using whitespace
        String[] tokens = tokenize(text);
        log.debug("[DocumentChunker] Tokenized text - tokenCount: {}", tokens.length);
        
        List<String> chunks = new ArrayList<>();
        int currentPosition = 0;
        
        while (currentPosition < tokens.length) {
            // Determine end position for this chunk
            int endPosition = Math.min(currentPosition + chunkSize, tokens.length);
            
            // Extract chunk tokens
            String[] chunkTokens = new String[endPosition - currentPosition];
            System.arraycopy(tokens, currentPosition, chunkTokens, 0, chunkTokens.length);
            
            // Reconstruct text from tokens
            String chunkText = String.join(" ", chunkTokens);
            
            // Adjust chunk boundary to preserve sentence endings
            if (endPosition < tokens.length) {
                chunkText = adjustToSentenceBoundary(chunkText, tokens, currentPosition, endPosition);
            }
            
            chunks.add(chunkText);
            log.debug("[DocumentChunker] Created text chunk - chunkNumber: {}, contentLength: {} chars", 
                     chunks.size(), chunkText.length());
            
            // Move to next chunk position with overlap
            currentPosition += (chunkSize - overlap);
            
            // Prevent infinite loop if overlap >= chunkSize
            if (currentPosition <= currentPosition - (chunkSize - overlap)) {
                currentPosition = endPosition;
            }
        }
        
        log.info("[DocumentChunker] Text chunking completed - totalChunks: {}, timestamp: {}", 
                chunks.size(), LocalDateTime.now());
        
        return chunks;
    }
    
    /**
     * Tokenize text using whitespace splitting.
     * Simple tokenization approach suitable for chunk size estimation.
     * 
     * @param text The text to tokenize
     * @return Array of tokens
     */
    private String[] tokenize(String text) {
        // Split on whitespace and filter empty strings
        return text.trim().split("\\s+");
    }
    
    /**
     * Count tokens in text using whitespace splitting.
     * 
     * @param text The text to count tokens in
     * @return Number of tokens
     */
    private int countTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return tokenize(text).length;
    }
    
    /**
     * Adjust chunk text to end at a sentence boundary when possible.
     * Searches backward from the end of the chunk to find the last sentence ending.
     * If no sentence boundary is found within reasonable distance, returns original text.
     * 
     * @param chunkText The current chunk text
     * @param allTokens All document tokens
     * @param startPos Start position in token array
     * @param endPos End position in token array
     * @return Adjusted chunk text ending at sentence boundary
     */
    private String adjustToSentenceBoundary(String chunkText, String[] allTokens, 
                                           int startPos, int endPos) {
        // Find all sentence boundaries in the chunk
        Matcher matcher = SENTENCE_BOUNDARY.matcher(chunkText);
        int lastSentenceEnd = -1;
        
        while (matcher.find()) {
            lastSentenceEnd = matcher.end();
        }
        
        // If we found a sentence boundary and it's not too far from the end
        // (at least 50% of chunk size), use it
        if (lastSentenceEnd > 0 && lastSentenceEnd >= chunkText.length() * 0.5) {
            return chunkText.substring(0, lastSentenceEnd).trim();
        }
        
        // Otherwise, return the full chunk
        return chunkText;
    }
}
