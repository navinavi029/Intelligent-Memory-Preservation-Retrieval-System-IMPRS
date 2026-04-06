package com.example.demo.repository;

import com.example.demo.model.Document;
import com.example.demo.model.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Document entity providing CRUD operations and custom queries.
 * Extends JpaRepository to inherit standard data access methods.
 * 
 * Validates Requirements 8.1, 8.4
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * Find all documents ordered by upload timestamp descending.
     * 
     * @return List of all documents, newest first
     */
    List<Document> findAllByOrderByUploadTimestampDesc();
    
    /**
     * Find all documents with a specific processing status.
     * 
     * @param status The processing status to filter by
     * @return List of documents matching the status
     */
    List<Document> findByStatus(ProcessingStatus status);
    
    /**
     * Find all documents with a specific processing status ordered by upload timestamp descending.
     * 
     * @param status The processing status to filter by
     * @return List of documents matching the status, newest first
     */
    List<Document> findByStatusOrderByUploadTimestampDesc(ProcessingStatus status);
}
