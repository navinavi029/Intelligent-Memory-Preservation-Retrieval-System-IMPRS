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
     * Configures separate caches for embeddings and chat responses.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "queryEmbeddings", 
                "chatResponses"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000) // Max 1000 entries per cache
                .expireAfterWrite(1, TimeUnit.HOURS) // Expire after 1 hour
                .recordStats()); // Enable statistics for monitoring
        
        return cacheManager;
    }
}
