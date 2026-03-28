package com.example.demo.service;

import com.example.demo.dto.ChatResponse;

/**
 * Service interface for processing user queries and generating responses using RAG.
 * Orchestrates the full query processing workflow: embedding generation, retrieval,
 * context construction, and LLM response generation.
 * 
 * Validates Requirements 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.5
 */
public interface ChatService {
    
    /**
     * Process user query and generate response using RAG.
     * 
     * Workflow:
     * 1. Sanitize user input to prevent prompt injection
     * 2. Generate query embedding using EmbeddingService
     * 3. Retrieve relevant chunks using RetrievalService
     * 4. Construct context window from retrieved chunks
     * 5. Build system prompt instructing model to answer only from context
     * 6. Call Gemini API with context and query
     * 7. Handle case when no relevant chunks found
     * 8. Include source references in response
     * 
     * @param query User's question
     * @return Chat response with answer and sources
     * @throws RuntimeException if API call fails after retry
     */
    ChatResponse processQuery(String query);
}
