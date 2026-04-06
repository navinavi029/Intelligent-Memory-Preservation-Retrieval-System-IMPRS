package com.example.demo.service;

import com.example.demo.dto.TextDocumentRequest;
import com.example.demo.model.Document;
import com.example.demo.model.DocumentChunk;
import com.example.demo.model.ProcessingStatus;
import com.example.demo.repository.ChunkRepository;
import com.example.demo.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of TextProcessingService for handling simple diary entries.
 * Processes single sentence entries without complex chunking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TextProcessingServiceImpl implements TextProcessingService {
    
    private static final int MAX_ENTRY_LENGTH = 500; // 500 characters max
    
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    
    /**
     * Process submitted diary entry with validation.
     * Creates document record and generates embedding for the single entry.
     * 
     * @param request The diary entry request
     * @return Document entity with COMPLETED status (no async processing needed)
     * @throws IllegalArgumentException if validation fails
     */
    @Override
    @Transactional
    public Document processTextDocument(TextDocumentRequest request) {
        log.info("[TextProcessingService] Memory sharing - content length: {} characters, timestamp: {}", 
                request.getMemory().length(), LocalDateTime.now());
        
        // Validate memory content
        if (request.getMemory() == null || request.getMemory().trim().isEmpty()) {
            log.error("[TextProcessingService] Memory processing failed - reason: empty content, timestamp: {}", 
                     LocalDateTime.now());
            throw new IllegalArgumentException("Please share a memory with me");
        }
        
        if (request.getMemory().length() > MAX_ENTRY_LENGTH) {
            log.error("[TextProcessingService] Memory processing failed - content length: {} characters, reason: exceeds maximum {} characters, timestamp: {}", 
                     request.getMemory().length(), MAX_ENTRY_LENGTH, LocalDateTime.now());
            throw new IllegalArgumentException(
                String.format("Let's keep your memory under %d characters so it's easy to remember", MAX_ENTRY_LENGTH)
            );
        }
        
        log.debug("[TextProcessingService] Memory validation passed - content length: {} characters", 
                 request.getMemory().length());
        
        // Create memory record
        String memoryPreview = request.getMemory().length() > 50 
            ? request.getMemory().substring(0, 50) + "..." 
            : request.getMemory();
            
        Document document = Document.builder()
                .filename(memoryPreview) // Use memory preview as filename
                .originalFilename(memoryPreview)
                .fileSize((long) request.getMemory().length()) // Store character count
                .uploadTimestamp(LocalDateTime.now())
                .status(ProcessingStatus.PROCESSING)
                .build();
        
        document = documentRepository.save(document);
        log.info("[TextProcessingService] Precious memory accepted - memoryId: {}, preview: '{}', content length: {} characters, timestamp: {}", 
                document.getId(), memoryPreview, request.getMemory().length(), LocalDateTime.now());
        
        try {
            // Generate embedding for the precious memory (no chunking needed)
            float[] embedding = embeddingService.generateQueryEmbedding(request.getMemory().trim());
            log.info("[TextProcessingService] Generated embedding for memory - memoryId: {}, dimensions: {}", 
                    document.getId(), embedding.length);
            
            // Create single chunk for the entry
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .chunkNumber(1)
                    .content(request.getMemory().trim())
                    .build();
            
            // Set embedding directly
            chunk.setEmbedding(embedding);
            
            chunkRepository.save(chunk);
            
            // Update document with completion status
            document.setStatus(ProcessingStatus.COMPLETED);
            document.setChunkCount(1);
            documentRepository.save(document);
            
            log.info("[TextProcessingService] Successfully completed processing for documentId: {}, status: COMPLETED, timestamp: {}", 
                    document.getId(), LocalDateTime.now());
            
            return document;
            
        } catch (Exception e) {
            log.error("[TextProcessingService] Failed to process entry for documentId: {}, error: {}", 
                     document.getId(), e.getMessage(), e);
            
            // Update document status to FAILED
            document.setStatus(ProcessingStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            
            throw new RuntimeException("Failed to process diary entry", e);
        }
    }
}