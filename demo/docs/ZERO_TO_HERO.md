# Zero-to-Hero Learning Path

## Part I: Foundations

### What is RAG (Retrieval-Augmented Generation)?

RAG combines two powerful AI techniques:
1. **Retrieval**: Finding relevant information from a knowledge base
2. **Generation**: Using an LLM to generate natural language responses

**Why RAG?** LLMs have knowledge cutoff dates and can't access your private documents. RAG solves this by:
- Retrieving relevant context from your documents
- Feeding that context to the LLM
- Getting accurate, grounded responses

### Key Technologies

#### Spring Boot 3.5
Modern Java framework for building production-ready applications.

**If you know Express.js (Node.js)**:
```javascript
// Express.js
app.post('/api/chat', async (req, res) => {
  const result = await chatService.processQuery(req.body.query);
  res.json(result);
});
```

**Spring Boot equivalent**:
```java
@PostMapping("/api/chat/query")
public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    ChatResponse response = chatService.processQuery(request.getQuery());
    return ResponseEntity.ok(response);
}
```

#### PostgreSQL + pgvector
Relational database with vector similarity search extension.

**Vector embeddings**: Text converted to arrays of numbers (2048 dimensions)
- "machine learning" → [0.23, -0.45, 0.67, ...]
- "artificial intelligence" → [0.25, -0.43, 0.69, ...]
- Similar concepts have similar vectors (cosine similarity)

#### NVIDIA NIM (NVIDIA Inference Microservices)
Cloud API for:
- **Embeddings**: Convert text to vectors (llama-3.2-nemoretriever)
- **Chat**: Generate responses (llama-3.1-8b-instruct)

#### Resilience4j
Library for building resilient applications:
- **Circuit Breaker**: Stop calling failing services
- **Retry**: Automatically retry failed operations
- **Rate Limiter**: Control request rate

### Architecture Patterns

#### 1. Async Processing
**Problem**: PDF processing takes 30-120 seconds  
**Solution**: Return immediately, process in background

```
Client                    Server
  |                         |
  |--POST /api/documents--->|
  |<---202 Accepted---------|  (immediate)
  |    {documentId: 123}    |
  |                         |
  |                         |--- Background Processing --->
  |                         |    (Extract, Chunk, Embed)
  |                         |
  |--GET /status/123------->|
  |<---200 OK---------------|
  |    {status: COMPLETED}  |
```

#### 2. Circuit Breaker Pattern
**Problem**: External API down → cascading failures  
**Solution**: Open circuit after failures, provide fallback

```
State Machine:
CLOSED (normal) --[50% failures]--> OPEN (failing fast)
OPEN --[30 seconds]--> HALF_OPEN (testing)
HALF_OPEN --[success]--> CLOSED
HALF_OPEN --[failure]--> OPEN
```

#### 3. Caching Strategy
**Problem**: Repeated queries cost money and time  
**Solution**: Cache embeddings and responses

```
Query: "What is machine learning?"
├─ Check cache → MISS
├─ Generate embedding → NVIDIA API call
├─ Vector search → PostgreSQL
├─ Generate response → NVIDIA API call
└─ Store in cache (1 hour TTL)

Same query again:
├─ Check cache → HIT
└─ Return cached response (< 10ms)
```

## Part II: This Codebase

### Project Structure

```
demo/
├── src/main/java/com/example/demo/
│   ├── config/          # Configuration classes
│   │   ├── AsyncConfig.java         # Thread pool for async
│   │   ├── CacheConfig.java         # Caffeine cache setup
│   │   ├── ResilienceConfig.java    # Circuit breaker config
│   │   ├── SecurityConfig.java      # Security + CORS
│   │   └── NvidiaConfig.java        # NVIDIA API settings
│   │
│   ├── controller/      # REST API endpoints
│   │   ├── UploadController.java    # Document upload
│   │   └── ChatController.java      # Chat queries
│   │
│   ├── service/         # Business logic
│   │   ├── PdfProcessingServiceImpl.java    # PDF → chunks
│   │   ├── EmbeddingServiceImpl.java        # Text → vectors
│   │   ├── RetrievalServiceImpl.java        # Vector search
│   │   ├── ChatServiceImpl.java             # RAG orchestration
│   │   └── ResilientNvidiaChatClient.java   # Resilient API client
│   │
│   ├── model/           # Domain entities
│   │   ├── Document.java            # PDF metadata
│   │   └── DocumentChunk.java       # Text chunk + embedding
│   │
│   ├── repository/      # Database access
│   │   ├── DocumentRepository.java
│   │   └── ChunkRepository.java     # Vector search queries
│   │
│   └── dto/             # API request/response objects
│
├── src/main/resources/
│   ├── application.properties       # Configuration
│   └── schema.sql                   # Database schema
│
└── docs/
    ├── ARCHITECTURE_IMPROVEMENTS.md
    └── ADR-001-async-processing-and-resilience.md
```

### The RAG Flow

#### Step 1: Document Upload
```
POST /api/documents
├─ Validate file (PDF, < 10MB)
├─ Save metadata to database (status: PENDING)
├─ Return 202 Accepted with documentId
└─ Trigger async processing
```

**File**: `UploadController.java:55`

#### Step 2: Async Processing
```
@Async("documentProcessingExecutor")
├─ Extract text from PDF (Apache PDFBox)
├─ Chunk text (500 tokens, 50 overlap)
├─ Generate embeddings (batch of 100)
│   └─ Call NVIDIA API with retry
├─ Store chunks + embeddings in PostgreSQL
└─ Update document status to COMPLETED
```

**File**: `PdfProcessingServiceImpl.java:111`

#### Step 3: Query Processing
```
POST /api/chat/query
├─ Sanitize input
├─ Generate query embedding (@Cacheable)
├─ Vector search (top 5 similar chunks)
│   └─ SELECT * FROM document_chunks
│       ORDER BY embedding <=> query_vector
│       LIMIT 5
├─ Construct context window
├─ Generate response with circuit breaker
│   └─ Call NVIDIA LLM with retry
└─ Return response + source references
```

**File**: `ChatServiceImpl.java:47`

### Key Classes Explained

#### ChatServiceImpl
The orchestrator of the RAG pipeline.

```java
@Override
public ChatResponse processQuery(String query) {
    // 1. Sanitize input
    String sanitized = sanitizeInput(query);
    
    // 2. Retrieve relevant chunks (vector search)
    List<RetrievedChunk> chunks = retrievalService.retrieveRelevantChunks(sanitized);
    
    // 3. Build context from chunks
    String context = constructContextWindow(chunks);
    
    // 4. Generate response with LLM
    String response = resilientChatClient.generateResponse(context, sanitized);
    
    // 5. Return with source references
    return new ChatResponse(response, extractSources(chunks));
}
```

#### ResilientNvidiaChatClient
Wraps NVIDIA API calls with resilience patterns.

```java
@CircuitBreaker(name = "nvidia-api", fallbackMethod = "fallbackResponse")
@Retry(name = "nvidia-api")
@RateLimiter(name = "nvidia-api")
public String generateResponse(String context, String query) {
    return nvidiaChatClient.generateResponse(systemPrompt, userMessage);
}

private String fallbackResponse(String context, String query, Exception e) {
    return "Service temporarily unavailable. Please try again.";
}
```

#### EmbeddingServiceImpl
Generates vector embeddings with caching.

```java
@Override
@Cacheable(value = "queryEmbeddings", key = "#query")
public float[] generateQueryEmbedding(String query) {
    List<float[]> embeddings = nvidiaEmbeddingClient.generateEmbeddings(
        Collections.singletonList(query)
    );
    return embeddings.get(0);
}
```

### Database Schema

```sql
-- Documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255),
    status VARCHAR(20),  -- PENDING, PROCESSING, COMPLETED, FAILED
    chunk_count INTEGER
);

-- Chunks with vector embeddings
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT REFERENCES documents(id),
    chunk_number INTEGER,
    content TEXT,
    embedding vector(2048),  -- 2048-dimensional vector
    UNIQUE (document_id, chunk_number)
);

-- Vector similarity index (IVFFlat)
CREATE INDEX idx_chunks_embedding 
ON document_chunks 
USING ivfflat (embedding vector_cosine_ops);
```

**File**: `schema.sql:1`

## Part III: Development Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 14+ with pgvector extension
- NVIDIA API key

### Setup Steps

1. **Install PostgreSQL + pgvector**
```bash
# Windows (using PostgreSQL installer)
# Then install pgvector extension
psql -U postgres
CREATE EXTENSION vector;
```

2. **Create Database**
```bash
psql -U postgres -f demo/setup-db.sql
```

3. **Configure Application**
```properties
# demo/src/main/resources/application.properties
nvidia.api.key=nvapi-YOUR_KEY_HERE
spring.datasource.username=postgres
spring.datasource.password=your_password
```

4. **Build and Run**
```bash
cd demo
mvnw clean install
mvnw spring-boot:run
```

5. **Test the API**
```bash
# Upload a PDF
curl -X POST http://localhost:8080/api/documents \
  -F "file=@test.pdf"

# Check status
curl http://localhost:8080/api/documents/1/status

# Query
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is this document about?"}'
```

### Running Tests
```bash
mvnw test
```

### Monitoring
```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

## Part IV: Contributing

### Code Style
- Follow Spring Boot conventions
- Use Lombok for boilerplate reduction
- Write meaningful variable names
- Add JavaDoc for public methods

### Adding a New Feature

1. **Create a branch**
```bash
git checkout -b feature/my-feature
```

2. **Write tests first (TDD)**
```java
@Test
void shouldProcessQuery() {
    // Given
    String query = "test query";
    
    // When
    ChatResponse response = chatService.processQuery(query);
    
    // Then
    assertNotNull(response);
    assertFalse(response.getResponse().isEmpty());
}
```

3. **Implement the feature**

4. **Run tests and build**
```bash
mvnw clean test
mvnw clean install
```

5. **Submit pull request**

### Debugging Tips

**Enable debug logging**:
```properties
logging.level.com.example.demo=DEBUG
logging.level.org.springframework.ai=DEBUG
```

**Check circuit breaker state**:
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

**View cache statistics**:
```bash
curl http://localhost:8080/actuator/metrics/cache.gets
```

## Appendices

### A. Glossary (40+ Terms)

See [GLOSSARY.md](./GLOSSARY.md) for complete definitions.

Key terms:
- **RAG**: Retrieval-Augmented Generation
- **Embedding**: Vector representation of text
- **Cosine Similarity**: Measure of vector similarity
- **Circuit Breaker**: Resilience pattern for failing services
- **Chunking**: Splitting documents into smaller pieces
- **Vector Database**: Database optimized for similarity search

### B. Key Files Reference

| File | Purpose | When to Edit |
|------|---------|--------------|
| `application.properties` | Configuration | Changing API keys, tuning |
| `ChatServiceImpl.java` | RAG logic | Modifying query flow |
| `SecurityConfig.java` | Security settings | Adding CORS origins |
| `ResilienceConfig.java` | Resilience tuning | Adjusting circuit breaker |
| `schema.sql` | Database schema | Adding tables/indexes |

### C. Common Tasks

**Change chunk size**:
```properties
# application.properties
app.chunking.chunk-size=1000  # Default: 500
app.chunking.overlap=100      # Default: 50
```

**Adjust cache TTL**:
```java
// CacheConfig.java
.expireAfterWrite(30, TimeUnit.MINUTES)  // Default: 1 hour
```

**Tune circuit breaker**:
```properties
# application.properties
resilience4j.circuitbreaker.instances.nvidia-api.failure-rate-threshold=30
```

**Add new CORS origin**:
```java
// SecurityConfig.java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "https://yourdomain.com"
));
```

**References**:
- Setup: `demo/setup-db.sql:1`
- Config: `demo/src/main/resources/application.properties:1`
- Architecture: `demo/docs/ARCHITECTURE_IMPROVEMENTS.md`
- Testing: `demo/TESTING_CHECKLIST.md`
