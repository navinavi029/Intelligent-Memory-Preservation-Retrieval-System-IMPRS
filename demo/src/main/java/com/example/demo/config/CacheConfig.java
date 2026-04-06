package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for caching using Caffeine.
 * Reduces API costs and improves response times for repeated queries.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Cache manager with Caffeine implementation.
     * Optimized caches for embeddings, chat responses, and retrieval results.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "queryEmbeddings", 
                "chatResponses",
                "retrievalResults",
                "documentChunks"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000) // Increased cache size
                .expireAfterWrite(2, TimeUnit.HOURS) // Longer expiration
                .expireAfterAccess(30, TimeUnit.MINUTES) // Access-based expiration
                .recordStats()); // Enable statistics for monitoring
        
        return cacheManager;
    }
}
