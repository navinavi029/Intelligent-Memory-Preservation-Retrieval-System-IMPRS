# ADR-001: Asynchronous Processing and Resilience Patterns

## Status
Accepted

## Date
2026-03-31

## Context
The PDF RAG Chatbot application initially implemented synchronous document processing and direct API calls to NVIDIA services. This approach created several production-readiness concerns:

1. **Blocking HTTP Threads**: PDF processing (text extraction, chunking, embedding generation) could take minutes for large documents, blocking HTTP request threads and risking timeouts.

2. **Cascading Failures**: Direct API calls to NVIDIA without circuit breaker protection meant that when the external API experienced issues, our application would continue making failed requests, wasting resources and degrading user experience.

3. **No Caching**: Every identical query regenerated embeddings and called the LLM, resulting in unnecessary API costs and increased latency.

4. **Weak Security**: CORS allowed all origins (`*`), no rate limiting, and API keys stored in properties files.

## Decision

We have implemented the following architectural improvements:

### 1. Asynchronous Document Processing
- Introduced `@Async` processing for PDF documents using Spring's async support
- Created dedicated thread pool (`documentProcessingExecutor`) for background processing
- Upload endpoint now returns immediately with `202 Accepted` and document ID
- Clients poll `/api/documents/{id}/status` to check processing status

### 2. Resilience Patterns with Resilience4j
- **Circuit Breaker**: Prevents cascading failures when NVIDIA API is down
  - Opens after 50% failure rate in sliding window of 10 calls
  - Waits 30 seconds before attempting recovery
  - Provides fallback responses when circuit is open
  
- **Retry with Exponential Backoff**: Automatically retries failed API calls
  - Maximum 3 attempts
  - Exponential backoff with 2x multiplier
  - Reduces load on recovering services
  
- **Rate Limiter**: Prevents overwhelming NVIDIA API
  - 10 requests per second limit
  - Protects against burst traffic
  - 5-second timeout for acquiring permission

### 3. Multi-Level Caching with Caffeine
- **Query Embeddings Cache**: Caches generated embeddings for identical queries
  - Maximum 1000 entries
  - 1-hour TTL
  - Reduces NVIDIA API calls and costs
  
- **Chat Responses Cache**: Caches complete responses for repeated questions
  - Same configuration as embeddings cache
  - Improves response time for common queries

### 4. Enhanced Security
- **Spring Security Integration**: Added security filter chain with headers
  - Content Security Policy
  - X-Frame-Options: DENY
  - XSS Protection headers
  
- **Restricted CORS**: Changed from `allowedOrigins("*")` to specific domains
  - Localhost for development
  - Production domains to be configured
  
- **Rate Limiting with Bucket4j**: API-level rate limiting
  - 100 requests per minute per IP
  - Prevents abuse and DoS attacks
  
- **Actuator Endpoints**: Added Prometheus metrics and health checks
  - `/actuator/health` for liveness/readiness probes
  - `/actuator/metrics` for application metrics
  - `/actuator/prometheus` for Prometheus scraping

## Consequences

### Positive
1. **Improved User Experience**: Upload endpoint responds immediately, no HTTP timeouts
2. **Better Resilience**: System gracefully handles external API failures
3. **Reduced Costs**: Caching eliminates redundant API calls to NVIDIA
4. **Enhanced Security**: Multiple layers of protection against abuse
5. **Production Ready**: Observability through metrics and health checks
6. **Scalability**: Async processing enables horizontal scaling

### Negative
1. **Increased Complexity**: More moving parts to monitor and debug
2. **Eventual Consistency**: Document processing status is eventually consistent
3. **Additional Dependencies**: Resilience4j, Caffeine, Spring Security, Bucket4j
4. **Configuration Overhead**: More properties to tune for optimal performance

### Neutral
1. **Client Changes Required**: Clients must implement polling for document status
2. **Cache Invalidation**: Need to consider cache invalidation strategies for updates
3. **Monitoring Required**: Circuit breaker states and cache hit rates need monitoring

## Implementation Notes

### Dependencies Added
```xml
- spring-boot-starter-security
- spring-boot-starter-cache
- spring-boot-starter-actuator
- resilience4j-spring-boot3
- resilience4j-circuitbreaker
- resilience4j-ratelimiter
- resilience4j-retry
- caffeine
- bucket4j-core
- micrometer-registry-prometheus
```

### Configuration Files
- `AsyncConfig.java`: Thread pool for async processing
- `ResilienceConfig.java`: Circuit breaker, retry, rate limiter
- `CacheConfig.java`: Caffeine cache configuration
- `SecurityConfig.java`: Spring Security and CORS
- `application.properties`: Resilience4j and Actuator settings

### Modified Services
- `PdfProcessingServiceImpl`: Now processes documents asynchronously
- `ChatServiceImpl`: Uses `ResilientNvidiaChatClient` wrapper
- `EmbeddingServiceImpl`: Added `@Cacheable` annotations
- `ResilientNvidiaChatClient`: New service wrapping NVIDIA client with resilience patterns

## Alternatives Considered

### 1. Message Queue (RabbitMQ/Kafka)
- **Pros**: Better decoupling, durable message storage, easier to scale
- **Cons**: Additional infrastructure, operational complexity
- **Decision**: Deferred to Phase 2 when scaling beyond single instance

### 2. Redis for Distributed Caching
- **Pros**: Shared cache across instances, persistence options
- **Cons**: Additional infrastructure, network latency
- **Decision**: Start with Caffeine (in-memory), migrate to Redis when scaling horizontally

### 3. API Gateway (Kong/AWS API Gateway)
- **Pros**: Centralized rate limiting, authentication, monitoring
- **Cons**: Additional infrastructure, single point of failure
- **Decision**: Use Spring Security and Bucket4j for now, consider gateway for microservices

## Future Improvements

1. **Event-Driven Architecture**: Replace async processing with event-driven pipeline
2. **Distributed Caching**: Migrate to Redis when scaling horizontally
3. **Authentication**: Add JWT/OAuth2 authentication for production
4. **Advanced Monitoring**: Add distributed tracing with OpenTelemetry
5. **Database Optimization**: Add connection pooling tuning and read replicas
6. **API Versioning**: Implement versioning strategy for backward compatibility

## References
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Async Documentation](https://spring.io/guides/gs/async-method/)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
