package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration properties for PDF RAG Chatbot.
 * Binds properties with prefix "app" from application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    
    private ChunkingConfig chunking = new ChunkingConfig();
    private RetrievalConfig retrieval = new RetrievalConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private RetryConfig retry = new RetryConfig();
    
    /**
     * Configuration for document chunking behavior.
     */
    @Data
    public static class ChunkingConfig {
        /**
         * Number of tokens per chunk (default: 500)
         */
        private int chunkSize = 500;
        
        /**
         * Number of overlapping tokens between chunks (default: 50)
         */
        private int overlap = 50;
    }
    
    /**
     * Configuration for retrieval and similarity search.
     */
    @Data
    public static class RetrievalConfig {
        /**
         * Number of top similar chunks to retrieve (default: 5)
         */
        private int topK = 5;
        
        /**
         * Minimum similarity threshold for chunk retrieval (default: 0.7)
         */
        private double similarityThreshold = 0.7;
    }
    
    /**
     * Configuration for embedding generation.
     */
    @Data
    public static class EmbeddingConfig {
        /**
         * Batch size for processing embeddings (default: 100)
         */
        private int batchSize = 100;
    }
    
    /**
     * Configuration for retry behavior on API failures.
     */
    @Data
    public static class RetryConfig {
        /**
         * Maximum number of retry attempts (default: 3)
         */
        private int maxAttempts = 3;
        
        /**
         * Initial delay in milliseconds before first retry (default: 1000)
         */
        private long initialDelay = 1000;
        
        /**
         * Multiplier for exponential backoff (default: 2.0)
         */
        private double multiplier = 2.0;
    }
}
