package com.example.demo.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resilient wrapper for NVIDIA chat client with circuit breaker, retry, and rate limiting.
 * Provides fallback responses when NVIDIA API is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientNvidiaChatClient {
    
    private final NvidiaChatClient nvidiaChatClient;
    
    private static final String FALLBACK_RESPONSE = 
        "I'm temporarily unable to process your request due to high demand. Please try again in a moment.";
    
    /**
     * Generate response with resilience patterns.
     * 
     * @param systemPrompt System prompt with context
     * @param userQuery User query
     * @return Generated response or fallback
     */
    @CircuitBreaker(name = "nvidia-api", fallbackMethod = "fallbackResponse")
    @Retry(name = "nvidia-api")
    @RateLimiter(name = "nvidia-api")
    public String generateResponse(String systemPrompt, String userQuery) {
        log.debug("[ResilientNvidiaChatClient] Calling NVIDIA API with circuit breaker protection");
        return nvidiaChatClient.generateResponse(systemPrompt, userQuery);
    }
    
    /**
     * Fallback method when circuit is open or all retries exhausted.
     * 
     * @param systemPrompt System prompt (unused in fallback)
     * @param userQuery User query (unused in fallback)
     * @param throwable The exception that triggered fallback
     * @return Fallback response
     */
    public String fallbackResponse(String systemPrompt, String userQuery, Throwable throwable) {
        log.warn("[ResilientNvidiaChatClient] Circuit breaker fallback triggered - error: {}, userQuery: '{}'", 
                throwable.getMessage(), userQuery.length() > 50 ? userQuery.substring(0, 50) + "..." : userQuery);
        
        // Log more details for debugging
        if (throwable.getCause() != null) {
            log.debug("[ResilientNvidiaChatClient] Root cause: {}", throwable.getCause().getMessage());
        }
        
        return FALLBACK_RESPONSE;
    }
}
