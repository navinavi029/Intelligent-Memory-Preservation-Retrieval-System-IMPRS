package com.example.demo.controller;

import com.example.demo.dto.DocumentMetadataDto;
import com.example.demo.dto.DocumentUploadResponse;
import com.example.demo.dto.ErrorResponse;
import com.example.demo.dto.ProcessingStatusDto;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for handling PDF document uploads and management operations.
 * Provides endpoints for uploading, listing, retrieving, and deleting documents.
 * 
 * Validates Requirements 1.1, 1.3, 1.4, 1.6, 8.1, 8.2, 8.3, 8.4, 8.5, 12.1, 12.3
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documents", description = "Endpoints for managing PDF document uploads, retrieval, and deletion")
public class UploadController {
    
    private final PdfProcessingService pdfProcessingService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    
    /**
     * Upload a PDF document for processing.
     * Validates file size (max 10MB) and type (PDF only).
     * 
     * @param file The PDF file to upload
     * @return DocumentUploadResponse with document ID and status
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload a PDF document",
        description = "Upload a PDF file for processing. The system extracts text content, " +
                     "splits it into chunks, generates embeddings using Google Gemini API, " +
                     "and stores them in the vector database for semantic search. " +
                     "Maximum file size is 10MB. Only PDF files are accepted."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Document uploaded and processed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentUploadResponse.class),
                examples = @ExampleObject(
                    name = "Successful upload",
                    value = """
                        {
                          "documentId": 123,
                          "filename": "research-paper.pdf",
                          "status": "COMPLETED",
                          "message": "Document uploaded and processed successfully"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - file is empty or invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentUploadResponse.class),
                examples = @ExampleObject(
                    name = "Empty file error",
                    value = """
                        {
                          "filename": "empty.pdf",
                          "message": "File cannot be empty"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "413",
            description = "Payload too large - file exceeds 10MB limit",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentUploadResponse.class),
                examples = @ExampleObject(
                    name = "File too large error",
                    value = """
                        {
                          "filename": "large-document.pdf",
                          "message": "File size exceeds maximum allowed size of 10MB"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Unsupported media type - only PDF files are accepted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentUploadResponse.class),
                examples = @ExampleObject(
                    name = "Invalid file type error",
                    value = """
                        {
                          "filename": "document.docx",
                          "message": "Only PDF files are supported"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - failed to process document",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentUploadResponse.class),
                examples = @ExampleObject(
                    name = "Processing error",
                    value = """
                        {
                          "filename": "corrupted.pdf",
                          "message": "Failed to process document: Unable to extract text from PDF"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @Parameter(
                description = "PDF file to upload (max 10MB)",
                required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received document upload request: {}", file.getOriginalFilename());
        
        try {
            // Process the document (validation happens in service layer)
            Document document = pdfProcessingService.processDocument(file);
            
            // Build response
            DocumentUploadResponse response = DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .filename(document.getFilename())
                    .status(document.getStatus())
                    .message("Document uploaded and processed successfully")
                    .build();
            
            log.info("Document uploaded successfully with ID: {}", document.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            // Handle validation errors (file size, type, empty file)
            log.warn("Document upload validation failed: {}", e.getMessage());
            
            DocumentUploadResponse errorResponse = DocumentUploadResponse.builder()
                    .filename(file.getOriginalFilename())
                    .message(e.getMessage())
                    .build();
            
            // Return appropriate HTTP status based on error type
            if (e.getMessage().contains("exceeds maximum")) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
            } else if (e.getMessage().contains("Only PDF files")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
        } catch (Exception e) {
            // Handle processing errors
            log.error("Document upload failed: {}", e.getMessage(), e);
            
            DocumentUploadResponse errorResponse = DocumentUploadResponse.builder()
                    .filename(file.getOriginalFilename())
                    .message("Failed to process document: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * List all uploaded documents with their metadata.
     * 
     * @return List of document metadata
     */
    @GetMapping
    @Operation(
        summary = "List all uploaded documents",
        description = "Retrieve a list of all uploaded documents with their metadata including " +
                     "filename, file size, upload timestamp, processing status, and chunk count."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved document list",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentMetadataDto.class),
                examples = @ExampleObject(
                    name = "Document list",
                    value = """
                        [
                          {
                            "id": 123,
                            "filename": "research-paper.pdf",
                            "fileSize": 2048576,
                            "uploadTimestamp": "2024-01-15T10:30:00",
                            "status": "COMPLETED",
                            "chunkCount": 42
                          },
                          {
                            "id": 124,
                            "filename": "technical-manual.pdf",
                            "fileSize": 5242880,
                            "uploadTimestamp": "2024-01-15T11:45:00",
                            "status": "COMPLETED",
                            "chunkCount": 87
                          }
                        ]
                        """
                )
            )
        )
    })
    public ResponseEntity<List<DocumentMetadataDto>> listDocuments() {
        log.info("Received request to list all documents");
        
        List<Document> documents = documentRepository.findAll();
        
        List<DocumentMetadataDto> documentList = documents.stream()
                .map(this::convertToMetadataDto)
                .collect(Collectors.toList());
        
        log.info("Returning {} documents", documentList.size());
        return ResponseEntity.ok(documentList);
    }
    
    /**
     * Get metadata for a specific document.
     * 
     * @param documentId The document ID
     * @return Document metadata
     */
    @GetMapping("/{documentId}")
    @Operation(
        summary = "Get document metadata",
        description = "Retrieve detailed metadata for a specific document by its ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved document metadata",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentMetadataDto.class),
                examples = @ExampleObject(
                    name = "Document metadata",
                    value = """
                        {
                          "id": 123,
                          "filename": "research-paper.pdf",
                          "fileSize": 2048576,
                          "uploadTimestamp": "2024-01-15T10:30:00",
                          "status": "COMPLETED",
                          "chunkCount": 42
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
                    name = "Not found error",
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
            @Parameter(description = "Unique identifier of the document", required = true, example = "123")
            @PathVariable Long documentId) {
        log.info("Received request to get document: {}", documentId);
        
        return documentRepository.findById(documentId)
                .map(document -> {
                    DocumentMetadataDto metadata = convertToMetadataDto(document);
                    log.info("Returning metadata for document: {}", documentId);
                    return ResponseEntity.ok(metadata);
                })
                .orElseGet(() -> {
                    log.warn("Document not found: {}", documentId);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Delete a document and all associated chunks and embeddings.
     * 
     * @param documentId The document ID to delete
     * @return Empty response with appropriate status
     */
    @DeleteMapping("/{documentId}")
    @Transactional
    @Operation(
        summary = "Delete a document",
        description = "Delete a document and all associated chunks and embeddings from the system. " +
                     "This operation is permanent and cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Document deleted successfully",
            content = @Content()
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Not found error",
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
            @Parameter(description = "Unique identifier of the document to delete", required = true, example = "123")
            @PathVariable Long documentId) {
        log.info("Received request to delete document: {}", documentId);
        
        return documentRepository.findById(documentId)
                .map(document -> {
                    // Delete all chunks (embeddings are deleted via cascade)
                    chunkRepository.deleteAll(document.getChunks());
                    
                    // Delete the document
                    documentRepository.delete(document);
                    
                    log.info("Successfully deleted document: {}", documentId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> {
                    log.warn("Document not found for deletion: {}", documentId);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Get processing status for a specific document.
     * 
     * @param documentId The document ID
     * @return Processing status information
     */
    @GetMapping("/{documentId}/status")
    @Operation(
        summary = "Get document processing status",
        description = "Retrieve the current processing status of a document including the number of " +
                     "chunks processed and any error messages if processing failed."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved processing status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProcessingStatusDto.class),
                examples = {
                    @ExampleObject(
                        name = "Completed status",
                        value = """
                            {
                              "documentId": 123,
                              "status": "COMPLETED",
                              "chunksProcessed": 42,
                              "errorMessage": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Failed status",
                        value = """
                            {
                              "documentId": 124,
                              "status": "FAILED",
                              "chunksProcessed": 0,
                              "errorMessage": "Failed to extract text from PDF: Corrupted file"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Document not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Not found error",
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
    public ResponseEntity<ProcessingStatusDto> getProcessingStatus(
            @Parameter(description = "Unique identifier of the document", required = true, example = "123")
            @PathVariable Long documentId) {
        log.info("Received request to get processing status for document: {}", documentId);
        
        return documentRepository.findById(documentId)
                .map(document -> {
                    ProcessingStatusDto statusDto = ProcessingStatusDto.builder()
                            .documentId(document.getId())
                            .status(document.getStatus())
                            .chunksProcessed(document.getChunkCount())
                            .errorMessage(document.getErrorMessage())
                            .build();
                    
                    log.info("Returning status for document {}: {}", documentId, document.getStatus());
                    return ResponseEntity.ok(statusDto);
                })
                .orElseGet(() -> {
                    log.warn("Document not found for status check: {}", documentId);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Convert Document entity to DocumentMetadataDto.
     * 
     * @param document The document entity
     * @return DocumentMetadataDto
     */
    private DocumentMetadataDto convertToMetadataDto(Document document) {
        return DocumentMetadataDto.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileSize(document.getFileSize())
                .uploadTimestamp(document.getUploadTimestamp())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .build();
    }
}
