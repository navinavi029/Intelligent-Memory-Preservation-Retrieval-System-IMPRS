package com.example.demo.controller;

import com.example.demo.dto.DocumentMetadataDto;
import com.example.demo.dto.DocumentUploadResponse;
import com.example.demo.dto.ErrorResponse;
import com.example.demo.dto.ProcessingStatusDto;
import com.example.demo.dto.TextDocumentRequest;
import com.example.demo.model.Document;
import com.example.demo.repository.ChunkRepository;
import com.example.demo.repository.DocumentRepository;
import com.example.demo.service.PdfProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for handling memory sharing and management operations.
 * Provides endpoints for sharing memory moments, listing, retrieving, and managing precious memories.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Memory Sharing", description = "Endpoints for sharing and managing precious memories and personal stories")
public class UploadController {
    
    private final PdfProcessingService pdfProcessingService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    
    /**
     * Share a precious memory moment for safekeeping.
     * Validates memory length (max 500 characters) to keep it focused and meaningful.
     * 
     * @param request The memory sharing request
     * @return DocumentUploadResponse with memory ID and status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Share a precious memory",
        description = "Share a meaningful memory moment for safekeeping and future recall. " +
                     "I'll help you remember this special moment whenever you ask about it. " +
                     "Maximum memory length is 500 characters to keep it focused."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Memory shared and safely stored",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentUploadResponse.class),
                examples = @ExampleObject(
                    name = "Successful memory sharing",
                    value = """
                        {
                          "documentId": 123,
                          "filename": "Had a wonderful visit with my grandchildren today...",
                          "status": "COMPLETED",
                          "message": "Your precious memory has been safely stored and is ready for you to explore anytime"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Please share a memory with me - it can't be empty or too long",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Validation failed",
                          "path": "/api/documents/submit",
                          "details": ["Please share a memory with me"]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - failed to process entry",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Processing error",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 500,
                          "error": "Internal Server Error",
                          "message": "Failed to process diary entry",
                          "path": "/api/documents/submit"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<DocumentUploadResponse> submitText(
            @Valid @RequestBody 
            @Parameter(description = "Memory sharing request with a meaningful moment")
            TextDocumentRequest request) {
        
        try {
            log.info("Received memory sharing - content length: {} characters", 
                    request.getMemory().length());
            
            Document document = pdfProcessingService.processTextDocument(request);
            
            DocumentUploadResponse response = DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .filename(document.getFilename())
                    .status(document.getStatus())
                    .message("Your precious memory has been safely stored and is ready for you to explore anytime")
                    .build();
            
            log.info("Successfully stored precious memory - memoryId: {}, preview: '{}'", 
                    document.getId(), document.getFilename());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Memory sharing failed due to validation error: {}", e.getMessage());
            throw e; // Let GlobalExceptionHandler handle it
            
        } catch (Exception e) {
            log.error("Failed to process memory sharing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process precious memory", e);
        }
    }
    
    /**
     * List all shared memories with their details.
     * Returns list of memories ordered by when they were shared (newest first).
     * 
     * @return List of DocumentMetadataDto with memory information
     */
    @GetMapping
    @Operation(
        summary = "List all shared memories",
        description = "See all the precious memories you've shared with me, including " +
                     "when you shared them and how I'm doing with keeping them safe. " +
                     "Your newest memories appear first."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully found all your shared memories",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentMetadataDto.class),
                examples = @ExampleObject(
                    name = "Your shared memories",
                    value = """
                        [
                          {
                            "id": 123,
                            "filename": "family-photos.pdf",
                            "originalFilename": "family-photos.pdf",
                            "fileSize": 2048576,
                            "uploadTimestamp": "2024-01-15T10:30:00",
                            "status": "COMPLETED",
                            "chunkCount": 25,
                            "errorMessage": null
                          },
                          {
                            "id": 122,
                            "filename": "Had a wonderful visit with my grandchildren today...",
                            "originalFilename": "Had a wonderful visit with my grandchildren today...",
                            "fileSize": 156,
                            "uploadTimestamp": "2024-01-15T09:15:00",
                            "status": "COMPLETED",
                            "chunkCount": 1,
                            "errorMessage": null
                          }
                        ]
                        """
                )
            )
        )
    })
    public ResponseEntity<List<DocumentMetadataDto>> listDocuments() {
        log.info("Received request to see all shared memories");
        
        List<Document> documents = documentRepository.findAllByOrderByUploadTimestampDesc();
        
        List<DocumentMetadataDto> documentDtos = documents.stream()
                .map(this::convertToMetadataDto)
                .collect(Collectors.toList());
        
        log.info("Successfully found {} precious memories", documentDtos.size());
        
        return ResponseEntity.ok(documentDtos);
    }
    
    /**
     * Get details of a specific document by ID.
     * Returns complete document metadata including processing status and error messages.
     * 
     * @param documentId The ID of the document to retrieve
     * @return DocumentMetadataDto with document details
     */
    @GetMapping("/{documentId}")
    @Operation(
        summary = "Get document details",
        description = "Retrieve detailed information about a specific document including " +
                     "processing status, chunk count, and any error messages."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved document details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentMetadataDto.class),
                examples = @ExampleObject(
                    name = "Document details response",
                    value = """
                        {
                          "id": 123,
                          "filename": "research-paper.pdf",
                          "originalFilename": "research-paper.pdf",
                          "fileSize": 2048576,
                          "uploadTimestamp": "2024-01-15T10:30:00",
                          "status": "COMPLETED",
                          "chunkCount": 25,
                          "errorMessage": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Document not found error",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Document not found with ID: 999",
                          "path": "/api/documents/999"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<DocumentMetadataDto> getDocument(
            @PathVariable 
            @Parameter(description = "The unique identifier of the document", example = "123")
            Long documentId) {
        
        log.info("Received request to get document with ID: {}", documentId);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("Document not found with ID: {}", documentId);
                    return new RuntimeException("Document not found with ID: " + documentId);
                });
        
        DocumentMetadataDto documentDto = convertToMetadataDto(document);
        
        log.info("Successfully retrieved document - ID: {}, filename: {}, status: {}", 
                document.getId(), document.getFilename(), document.getStatus());
        
        return ResponseEntity.ok(documentDto);
    }
    
    /**
     * Delete a document and all its associated chunks.
     * Removes the document record and all related chunk data from the database.
     * 
     * @param documentId The ID of the document to delete
     * @return HTTP 204 No Content on successful deletion
     */
    @DeleteMapping("/{documentId}")
    @Transactional
    @Operation(
        summary = "Delete a document",
        description = "Delete a document and all its associated chunks from the system. " +
                     "This operation is irreversible and will remove all processed data " +
                     "related to the document."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Document deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Document not found error",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Document not found with ID: 999",
                          "path": "/api/documents/999"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Void> deleteDocument(
            @PathVariable 
            @Parameter(description = "The unique identifier of the document to delete", example = "123")
            Long documentId) {
        
        log.info("Received request to delete document with ID: {}", documentId);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("Document not found for deletion with ID: {}", documentId);
                    return new RuntimeException("Document not found with ID: " + documentId);
                });
        
        // Delete all chunks first (cascade should handle this, but being explicit)
        int deletedChunks = chunkRepository.deleteByDocumentId(documentId);
        log.debug("Deleted {} chunks for document ID: {}", deletedChunks, documentId);
        
        // Delete the document
        documentRepository.delete(document);
        
        log.info("Successfully deleted document - ID: {}, filename: {}, chunks deleted: {}", 
                documentId, document.getFilename(), deletedChunks);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get processing status of a specific document.
     * Returns current processing status and any error messages.
     * 
     * @param documentId The ID of the document to check
     * @return ProcessingStatusDto with current status information
     */
    @GetMapping("/{documentId}/status")
    @Operation(
        summary = "Get document processing status",
        description = "Check the current processing status of a document. Useful for monitoring " +
                     "the progress of document processing and identifying any errors."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved processing status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProcessingStatusDto.class),
                examples = @ExampleObject(
                    name = "Processing status response",
                    value = """
                        {
                          "documentId": 123,
                          "filename": "research-paper.pdf",
                          "status": "COMPLETED",
                          "chunkCount": 25,
                          "errorMessage": null,
                          "uploadTimestamp": "2024-01-15T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Document not found error",
                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Document not found with ID: 999",
                          "path": "/api/documents/999/status"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ProcessingStatusDto> getDocumentStatus(
            @PathVariable 
            @Parameter(description = "The unique identifier of the document", example = "123")
            Long documentId) {
        
        log.info("Received request to get processing status for document ID: {}", documentId);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("Document not found for status check with ID: {}", documentId);
                    return new RuntimeException("Document not found with ID: " + documentId);
                });
        
        ProcessingStatusDto statusDto = ProcessingStatusDto.builder()
                .documentId(document.getId())
                .filename(document.getFilename())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .errorMessage(document.getErrorMessage())
                .uploadTimestamp(document.getUploadTimestamp())
                .build();
        
        log.info("Successfully retrieved processing status - ID: {}, filename: {}, status: {}", 
                document.getId(), document.getFilename(), document.getStatus());
        
        return ResponseEntity.ok(statusDto);
    }
    
    /**
     * Convert Document entity to DocumentMetadataDto.
     * 
     * @param document The document entity to convert
     * @return DocumentMetadataDto with document metadata
     */
    private DocumentMetadataDto convertToMetadataDto(Document document) {
        return DocumentMetadataDto.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .uploadTimestamp(document.getUploadTimestamp())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .errorMessage(document.getErrorMessage())
                .build();
    }
}