package com.example.demo.service;

import com.example.demo.model.Document;
import com.example.demo.model.DocumentChunk;
import com.example.demo.model.ProcessingStatus;
import com.example.demo.repository.ChunkRepository;
import com.example.demo.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of PdfProcessingService for handling PDF document processing.
 * Uses Spring AI's PagePdfDocumentReader for text extraction.
 * 
 * Validates Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 12.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingServiceImpl implements PdfProcessingService {
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    
    /**
     * Process uploaded PDF file with validation and text extraction.
     * Creates document record, validates file, then processes asynchronously.
     * 
     * @param file The uploaded PDF file
     * @return Document entity with PENDING status
     * @throws IllegalArgumentException if validation fails
     */
    @Override
    @Transactional
    public Document processDocument(MultipartFile file) {
        // Log PDF upload attempt with filename and size (Requirement 9.5)
        log.info("[PdfProcessingService] PDF upload attempt - filename: {}, size: {} bytes, timestamp: {}", 
                file.getOriginalFilename(), file.getSize(), LocalDateTime.now());
        
        // Validate file is not empty (Requirement 12.1)
        if (file.isEmpty()) {
            log.error("[PdfProcessingService] PDF upload failed - filename: {}, reason: empty file, timestamp: {}", 
                     file.getOriginalFilename(), LocalDateTime.now());
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        // Validate file size (Requirement 1.3)
        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("[PdfProcessingService] PDF upload failed - filename: {}, size: {} bytes, reason: exceeds maximum {} bytes, timestamp: {}", 
                     file.getOriginalFilename(), file.getSize(), MAX_FILE_SIZE, LocalDateTime.now());
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d MB", 
                             MAX_FILE_SIZE / (1024 * 1024))
            );
        }
        
        // Validate file type (Requirement 1.4)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(PDF_CONTENT_TYPE)) {
            log.error("[PdfProcessingService] PDF upload failed - filename: {}, contentType: {}, reason: invalid file type, timestamp: {}", 
                     file.getOriginalFilename(), contentType, LocalDateTime.now());
            throw new IllegalArgumentException("Only PDF files are accepted");
        }
        
        log.debug("[PdfProcessingService] File validation passed - filename: {}, size: {} bytes, contentType: {}", 
                 file.getOriginalFilename(), file.getSize(), contentType);
        
        // Create document record with PENDING status
        Document document = Document.builder()
                .filename(file.getOriginalFilename())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadTimestamp(LocalDateTime.now())
                .status(ProcessingStatus.PENDING)
                .build();
        
        document = documentRepository.save(document);
        log.info("[PdfProcessingService] PDF upload accepted - documentId: {}, filename: {}, size: {} bytes, status: PENDING, timestamp: {}", 
                document.getId(), file.getOriginalFilename(), file.getSize(), LocalDateTime.now());
        
        // Process document asynchronously
        processDocumentAsync(document.getId(), file);
        
        return document;
    }
    
    /**
     * Process document asynchronously in background.
     * Extracts text, chunks, generates embeddings, and updates status.
     * 
     * @param documentId The document ID
     * @param file The uploaded PDF file
     */
    @Async("documentProcessingExecutor")
    public void processDocumentAsync(Long documentId, MultipartFile file) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        try {
            // Update status to PROCESSING
            document.setStatus(ProcessingStatus.PROCESSING);
            document = documentRepository.save(document);
            log.info("[PdfProcessingService] Document processing started - documentId: {}, status: PROCESSING, timestamp: {}", 
                    document.getId(), LocalDateTime.now());
            
            // Extract text from PDF (Requirement 1.2)
            String extractedText = extractText(file);
            log.info("[PdfProcessingService] PDF text extraction successful - documentId: {}, filename: {}, extractedChars: {}, timestamp: {}", 
                    document.getId(), file.getOriginalFilename(), extractedText.length(), LocalDateTime.now());
            
            // Chunk the extracted text
            log.info("[PdfProcessingService] Starting text chunking - documentId: {}, timestamp: {}", 
                    document.getId(), LocalDateTime.now());
            List<DocumentChunk> chunks = documentChunker.chunkDocument(extractedText, document);
            log.info("[PdfProcessingService] Text chunking completed - documentId: {}, chunkCount: {}, timestamp: {}", 
                    document.getId(), chunks.size(), LocalDateTime.now());
            
            // Generate embeddings for chunks
            log.info("[PdfProcessingService] Starting embedding generation - documentId: {}, chunkCount: {}, timestamp: {}", 
                    document.getId(), chunks.size(), LocalDateTime.now());
            List<DocumentChunk> chunksWithEmbeddings = embeddingService.generateEmbeddings(chunks);
            log.info("[PdfProcessingService] Embedding generation completed - documentId: {}, timestamp: {}", 
                    document.getId(), LocalDateTime.now());
            
            // Save chunks with embeddings
            log.info("[PdfProcessingService] Saving chunks to database - documentId: {}, chunkCount: {}, timestamp: {}", 
                    document.getId(), chunksWithEmbeddings.size(), LocalDateTime.now());
            chunkRepository.saveAll(chunksWithEmbeddings);
            
            // Update document with chunk count and mark as COMPLETED
            document.setChunkCount(chunksWithEmbeddings.size());
            document.setStatus(ProcessingStatus.COMPLETED);
            document = documentRepository.save(document);
            log.info("[PdfProcessingService] PDF upload completed successfully - documentId: {}, filename: {}, size: {} bytes, chunkCount: {}, status: COMPLETED, outcome: SUCCESS, timestamp: {}", 
                    document.getId(), file.getOriginalFilename(), file.getSize(), chunksWithEmbeddings.size(), LocalDateTime.now());
            
        } catch (Exception e) {
            // Update status to FAILED with error message (Requirement 1.5, 9.1)
            log.error("[PdfProcessingService] PDF processing failed - documentId: {}, filename: {}, component: PdfProcessingService, outcome: FAILED, timestamp: {}, error: {}", 
                     document.getId(), file.getOriginalFilename(), LocalDateTime.now(), e.getMessage(), e);
            document.setStatus(ProcessingStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
    }
    
    /**
     * Extract text content from PDF using Spring AI PagePdfDocumentReader.
     * 
     * @param file The PDF file
     * @return Extracted text content
     * @throws RuntimeException if extraction fails
     */
    @Override
    public String extractText(MultipartFile file) {
        log.debug("[PdfProcessingService] Starting text extraction - filename: {}, size: {} bytes, timestamp: {}", 
                 file.getOriginalFilename(), file.getSize(), LocalDateTime.now());
        
        try {
            // Convert MultipartFile to ByteArrayResource for Spring AI reader
            byte[] fileBytes = file.getBytes();
            log.debug("[PdfProcessingService] File bytes loaded - filename: {}, byteCount: {}", 
                     file.getOriginalFilename(), fileBytes.length);
            
            ByteArrayResource resource = new ByteArrayResource(fileBytes);
            
            // Use Spring AI PagePdfDocumentReader for text extraction
            log.debug("[PdfProcessingService] Initializing PDF reader - filename: {}", file.getOriginalFilename());
            DocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<org.springframework.ai.document.Document> documents = pdfReader.get();
            
            log.debug("[PdfProcessingService] PDF pages extracted - filename: {}, pageCount: {}", 
                     file.getOriginalFilename(), documents.size());
            
            // Combine all page contents into single text
            String extractedText = documents.stream()
                    .map(org.springframework.ai.document.Document::getText)
                    .collect(Collectors.joining("\n"));
            
            if (extractedText.trim().isEmpty()) {
                log.error("[PdfProcessingService] Text extraction failed - filename: {}, component: PdfProcessingService, reason: no text content found, timestamp: {}", 
                         file.getOriginalFilename(), LocalDateTime.now());
                throw new RuntimeException("No text content could be extracted from PDF");
            }
            
            log.debug("[PdfProcessingService] Text extraction completed - filename: {}, extractedChars: {}, timestamp: {}", 
                     file.getOriginalFilename(), extractedText.length(), LocalDateTime.now());
            return extractedText;
            
        } catch (IOException e) {
            log.error("[PdfProcessingService] Text extraction failed - filename: {}, component: PdfProcessingService, error: IO error, timestamp: {}, message: {}", 
                     file.getOriginalFilename(), LocalDateTime.now(), e.getMessage(), e);
            throw new RuntimeException("Failed to read PDF file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[PdfProcessingService] Text extraction failed - filename: {}, component: PdfProcessingService, timestamp: {}, error: {}", 
                     file.getOriginalFilename(), LocalDateTime.now(), e.getMessage(), e);
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
}
