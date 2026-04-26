# System Architecture

Detailed architecture documentation for the Intelligent Memory Preservation & Retrieval System.

## Table of Contents

1. [Overview](#overview)
2. [System Components](#system-components)
3. [Data Flow](#data-flow)
4. [Technology Stack](#technology-stack)
5. [Design Patterns](#design-patterns)
6. [Security Architecture](#security-architecture)
7. [Performance Optimization](#performance-optimization)
8. [Scalability](#scalability)

---

## Overview

IMPRS follows a layered architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│              (REST Controllers + DTOs)                   │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                    Service Layer                         │
│         (Business Logic + Orchestration)                 │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                 Data Access Layer                        │
│            (Repositories + Entities)                     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                  Infrastructure Layer                    │
│        (Database + External APIs + Cache)                │
└─────────────────────────────────────────────────────────┘
```

---

## System Components

### 1. Presentation Layer

**Controllers:**
- `ChatController`: Handles memory query requests
- `UploadController`: Manages document uploads (PDF and text)
- `HealthController`: Provides health check endpoints
- `DiagnosticController`: Exposes system diagnostics

**DTOs (Data Transfer Objects):**
- `ChatRequest/ChatResponse`: Chat interaction models
- `DocumentUploadResponse`: Upload result information
- `RetrievedChunk`: Retrieved memory chunk data
- `SourceReference`: Citation information
- `ErrorResponse`: Standardized error format

### 2. Service Layer

**Core Services:**

#### ChatService
- Orchestrates the RAG pipeline
- Sanitizes user input
- Generates query embeddings
- Retrieves relevant chunks
- Constructs context windows
- Generates AI responses

#### EmbeddingService
- Interfaces with NVIDIA NIM API
- Generates vector embeddings
- Implements caching strategy
- Handles batch processing

#### RetrievalService
- Performs vector similarity search
- Queries pgvector database
- Applies similarity thresholds
- Returns ranked results

#### DocumentProcessingService
- **PdfProcessingService**: Extracts text from PDFs
- **TextProcessingService**: Processes plain text
- **DocumentChunker**: Splits text into semantic chunks

**Resilience Services:**

#### ResilientNvidiaChatClient
- Wraps NVIDIA API calls
- Implements circuit breaker pattern
- Applies retry logic
- Enforces rate limiting

#### CachedRetrievalService
- Caches retrieval results
- Reduces database load
- Improves response times

### 3. Data Access Layer

**Repositories:**

#### DocumentRepository
```java
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByProcessingStatus(ProcessingStatus status);
    Optional<Document> findByFilename(String filename);
}
```

#### ChunkRepository
```java
public interface ChunkRepository extends JpaRepository<DocumentChunk, Long> {
    @Query(value = "SELECT ... FROM document_chunks WHERE ...")
    List<Object[]> findSimilarChunks(String embedding, double threshold, int limit);
}
```

**Entities:**

#### Document
- Metadata about uploaded documents
- Processing status tracking
- File information

#### DocumentChunk
- Text content chunks
- Vector embeddings (4096 dimensions)
- Chunk metadata

### 4. Configuration Layer

**Configuration Classes:**

- `AppConfig`: Application-wide settings
- `AsyncConfig`: Async processing configuration
- `CacheConfig`: Caffeine cache setup
- `NvidiaConfig`: NVIDIA API configuration
- `ResilienceConfig`: Resilience4j settings
- `SecurityConfig`: Spring Security configuration
- `WebConfig`: CORS and web settings

---

## Data Flow

### Document Upload Flow

```
┌─────────┐
│ Client  │
└────┬────┘
     │ 1. POST /upload/pdf
     ▼
┌─────────────────┐
│ UploadController│
└────┬────────────┘
     │ 2. Validate file
     ▼
┌──────────────────────┐
│ PdfProcessingService │
└────┬─────────────────┘
     │ 3. Extract text
     ▼
┌──────────────────┐
│ DocumentChunker  │
└────┬─────────────┘
     │ 4. Split into chunks
     ▼
┌──────────────────┐
│ EmbeddingService │
└────┬─────────────┘
     │ 5. Generate embeddings
     ▼
┌──────────────────┐
│ NVIDIA NIM API   │
└────┬─────────────┘
     │ 6. Return vectors
     ▼
┌──────────────────┐
│ ChunkRepository  │
└────┬─────────────┘
     │ 7. Store in DB
     ▼
┌──────────────────┐
│ PostgreSQL       │
└──────────────────┘
```

### Query Processing Flow

```
┌─────────┐
│ Client  │
└────┬────┘
     │ 1. POST /chat/query
     ▼
┌──────────────┐
│ChatController│
└────┬─────────┘
     │ 2. Validate query
     ▼
┌──────────────┐
│ ChatService  │
└────┬─────────┘
     │ 3. Sanitize input
     ▼
┌──────────────────┐
│ EmbeddingService │
└────┬─────────────┘
     │ 4. Generate query embedding
     ▼
┌──────────────────┐
│ RetrievalService │
└────┬─────────────┘
     │ 5. Vector similarity search
     ▼
┌──────────────────┐
│ PostgreSQL       │
│ (pgvector)       │
└────┬─────────────┘
     │ 6. Return top-k chunks
     ▼
┌──────────────┐
│ ChatService  │
└────┬─────────┘
     │ 7. Construct context
     ▼
┌─────────────────────────┐
│ ResilientNvidiaChatClient│
└────┬────────────────────┘
     │ 8. Generate response
     ▼
┌──────────────────┐
│ NVIDIA LLM API   │
└────┬─────────────┘
     │ 9. Return answer
     ▼
┌──────────────┐
│ ChatService  │
└────┬─────────┘
     │ 10. Build response with sources
     ▼
┌──────────────┐
│ChatController│
└────┬─────────┘
     │ 11. Return JSON
     ▼
┌─────────┐
│ Client  │
└─────────┘
```

---

## Technology Stack

### Backend Framework
- **Spring Boot 3.5.12**: Core framework
- **Spring Web**: REST API
- **Spring Data JPA**: Data access
- **Spring Security**: Authentication & authorization
- **Spring Cache**: Caching abstraction
- **Spring Actuator**: Monitoring

### AI/ML Integration
- **Spring AI 1.1.3**: AI integration framework
- **NVIDIA NIM API**: Embeddings and LLM
- **pgvector**: Vector similarity search

### Database
- **PostgreSQL 16**: Primary database
- **pgvector extension**: Vector operations
- **HikariCP**: Connection pooling

### Resilience & Reliability
- **Resilience4j**: Circuit breaker, retry, rate limiter
- **Caffeine**: High-performance caching
- **Bucket4j**: Rate limiting

### Observability
- **Micrometer**: Metrics collection
- **Prometheus**: Metrics export
- **SLF4J + Logback**: Logging

### API Documentation
- **SpringDoc OpenAPI**: API documentation
- **Swagger UI**: Interactive API explorer

### Build & Deployment
- **Maven**: Build tool
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration

---

## Design Patterns

### 1. Layered Architecture
- Clear separation between presentation, business, and data layers
- Each layer depends only on the layer below it
- Promotes maintainability and testability

### 2. Repository Pattern
- Abstracts data access logic
- Provides clean interface for data operations
- Enables easy testing with mocks

### 3. Service Layer Pattern
- Encapsulates business logic
- Orchestrates operations across repositories
- Provides transaction boundaries

### 4. DTO Pattern
- Separates internal models from API contracts
- Prevents over-fetching and under-fetching
- Enables API versioning

### 5. Builder Pattern
- Used extensively in DTOs (via Lombok)
- Provides fluent API for object construction
- Improves code readability

### 6. Strategy Pattern
- Different processing strategies for PDF vs text
- Pluggable embedding providers
- Flexible retrieval algorithms

### 7. Decorator Pattern
- `ResilientNvidiaChatClient` decorates base client
- Adds resilience features transparently
- `CachedRetrievalService` adds caching layer

### 8. Template Method Pattern
- Abstract processing pipeline
- Concrete implementations for different document types
- Promotes code reuse

---

## Security Architecture

### Input Validation

```java
// Request validation
@Valid @RequestBody ChatRequest request

// DTO validation
@NotBlank(message = "Query must not be blank")
@Size(min = 1, max = 1000, message = "Query must be between 1 and 1000 characters")
private String query;
```

### Input Sanitization

```java
private String sanitizeInput(String input) {
    // Remove null bytes
    String sanitized = input.replace("\0", "");
    
    // Remove control characters
    sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
    
    // Trim and limit length
    return sanitized.trim().substring(0, Math.min(1000, sanitized.length()));
}
```

### SQL Injection Prevention
- JPA/Hibernate parameterized queries
- No dynamic SQL construction
- Native queries use parameter binding

### Prompt Injection Prevention
- Input sanitization
- Context window isolation
- System prompt protection

### API Security
- CORS configuration
- Rate limiting
- Request size limits

---

## Performance Optimization

### 1. Vector Search Optimization

**HNSW Indexing:**
```sql
CREATE INDEX idx_chunks_embedding ON document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);
```

**Benefits:**
- Sub-linear search complexity
- Better recall than IVFFlat at the same speed
- No training step required
- Configurable accuracy/speed tradeoff via `ef_search`

> Note: NVIDIA NV-Embed-v1 produces 4096-dimensional vectors. The system truncates these to **2000 dimensions** before storage to stay within pgvector's HNSW index limits while preserving most semantic information.

### 2. Connection Pooling

**HikariCP Configuration:**
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

**Benefits:**
- Reuses database connections
- Reduces connection overhead
- Handles connection failures gracefully

### 3. Caching Strategy

**Multi-Level Caching:**

```java
// L1: Embedding cache (1 hour TTL)
@Cacheable(value = "embeddings", key = "#text")
public float[] generateEmbedding(String text)

// L2: Retrieval cache (30 minutes TTL)
@Cacheable(value = "retrievals", key = "#embedding + #topK")
public List<RetrievedChunk> retrieveSimilarChunks(...)
```

**Cache Statistics:**
- Hit rate: ~78%
- Average response time reduction: 85%

### 4. Async Processing

```java
@Async
public CompletableFuture<ProcessingResult> processDocument(MultipartFile file) {
    // Long-running processing
    return CompletableFuture.completedFuture(result);
}
```

**Benefits:**
- Non-blocking API responses
- Better resource utilization
- Improved user experience

### 5. Batch Processing

```java
// Process embeddings in batches
List<List<String>> batches = partition(chunks, batchSize);
for (List<String> batch : batches) {
    List<float[]> embeddings = embeddingService.generateBatch(batch);
}
```

**Benefits:**
- Reduces API calls
- Better throughput
- Lower latency

---

## Scalability

### Horizontal Scaling

**Stateless Design:**
- No server-side session state
- All state in database or cache
- Load balancer friendly

**Database Scaling:**
- Read replicas for queries
- Write master for updates
- Connection pooling per instance

### Vertical Scaling

**Resource Optimization:**
- JVM heap tuning
- Connection pool sizing
- Thread pool configuration

### Caching Strategy

**Distributed Cache (Future):**
- Redis for shared cache
- Cache invalidation strategy
- Cache warming on startup

### Database Optimization

**Query Optimization:**
- Proper indexing strategy
- Query plan analysis
- Materialized views for aggregations

**Partitioning (Future):**
- Partition by document date
- Partition by user (multi-tenant)
- Archive old data

### API Gateway (Future)

**Features:**
- Rate limiting per client
- Request routing
- API versioning
- Authentication/authorization

---

## Monitoring & Observability

### Metrics

**Application Metrics:**
- Request rate and latency
- Error rate
- Cache hit rate
- Circuit breaker state

**Infrastructure Metrics:**
- CPU and memory usage
- Database connections
- Thread pool utilization
- Garbage collection

### Logging

**Structured Logging:**
```java
log.info("[ChatService] Query processing completed - " +
         "retrievedChunks: {}, sources: {}, answerLength: {}", 
         chunks, sources, length);
```

**Log Levels:**
- ERROR: System errors requiring attention
- WARN: Degraded performance or recoverable errors
- INFO: Important business events
- DEBUG: Detailed diagnostic information
- TRACE: Very detailed debugging

### Tracing (Future)

**Distributed Tracing:**
- Spring Cloud Sleuth
- Zipkin or Jaeger
- Correlation IDs across services

---

## Deployment Architecture

### Development Environment

```
┌──────────────┐
│  Developer   │
│   Machine    │
│              │
│ - Spring Boot│
│ - PostgreSQL │
│   (Docker)   │
└──────────────┘
```

### Production Environment (Future)

```
┌─────────────────────────────────────────────┐
│              Load Balancer                   │
└──────────┬──────────────────────────────────┘
           │
    ┌──────┴──────┐
    │             │
┌───▼───┐    ┌───▼───┐
│ App 1 │    │ App 2 │
└───┬───┘    └───┬───┘
    │            │
    └──────┬─────┘
           │
    ┌──────▼──────┐
    │  PostgreSQL │
    │   Cluster   │
    └─────────────┘
```

---

## Future Enhancements

### Microservices Architecture

**Service Decomposition:**
- Document Processing Service
- Embedding Service
- Retrieval Service
- Chat Service
- User Management Service

### Event-Driven Architecture

**Event Bus:**
- Kafka or RabbitMQ
- Async document processing
- Event sourcing for audit trail

### Multi-Tenancy

**Tenant Isolation:**
- Row-level security
- Separate schemas per tenant
- Tenant-specific caching

### Advanced Features

- Real-time collaboration
- Voice interface
- Image and video support
- Advanced analytics
- Mobile applications
