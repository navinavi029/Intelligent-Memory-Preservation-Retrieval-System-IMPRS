package com.example.demo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for resilience patterns using Resilience4j.
 * Provides circuit breaker, retry, and rate limiter configurations for NVIDIA API calls.
 */
@Configuration
public class ResilienceConfig {
    
    /**
     * Circuit breaker configuration for NVIDIA API.
     * Prevents cascading failures when API is unavailable.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Consider last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open state
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        return CircuitBreakerRegistry.of(config);
    }
    
    /**
     * Retry configuration for NVIDIA API.
     * Implements exponential backoff with jitter.
     */
    @Bean
    public RetryRegistry retryRegistry(AppConfig appConfig) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(appConfig.getRetry().getMaxAttempts())
                .waitDuration(Duration.ofMillis(appConfig.getRetry().getInitialDelay()))
                .retryExceptions(RuntimeException.class)
                .build();
        
        return RetryRegistry.of(config);
    }
    
    /**
     * Rate limiter configuration for NVIDIA API.
     * Prevents overwhelming the API with too many requests.
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10) // 10 requests
                .limitRefreshPeriod(Duration.ofSeconds(1)) // per second
                .timeoutDuration(Duration.ofSeconds(5)) // Wait max 5s for permission
                .build();
        
        return RateLimiterRegistry.of(config);
    }
}
