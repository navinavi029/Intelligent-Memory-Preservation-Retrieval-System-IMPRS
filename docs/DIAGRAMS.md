# System Diagrams

Visual representations of the IMPRS architecture and workflows.

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Data Flow Diagrams](#data-flow-diagrams)
3. [Sequence Diagrams](#sequence-diagrams)
4. [Component Diagrams](#component-diagrams)
5. [Deployment Diagrams](#deployment-diagrams)

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Web UI  │  │  Mobile  │  │   CLI    │  │  API     │   │
│  │ (Future) │  │ (Future) │  │ (Future) │  │ Clients  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS/REST
┌────────────────────────┴────────────────────────────────────┐
│                    API Gateway Layer                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Spring Boot REST API (Port 8080)                    │   │
│  │  - Rate Limiting (20 req/sec)                        │   │
│  │  - Input Validation                                  │   │
│  │  - Error Handling                                    │   │
│  │  - OpenAPI Documentation                             │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────────┐
│                   Application Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Upload     │  │     Chat     │  │   Health     │     │
│  │  Controller  │  │  Controller  │  │  Controller  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│  ┌──────┴──────────────────┴──────────────────┴───────┐    │
│  │              Service Layer                          │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐        │    │
│  │  │   PDF    │  │   Text   │  │   Chat   │        │    │
│  │  │Processing│  │Processing│  │  Service │        │    │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘        │    │
│  │       │             │              │               │    │
│  │  ┌────┴─────────────┴──────────────┴─────┐        │    │
│  │  │        Document Chunker                │        │    │
│  │  └────────────────┬───────────────────────┘        │    │
│  │                   │                                 │    │
│  │  ┌────────────────┴───────────────────────┐        │    │
│  │  │        Embedding Service               │        │    │
│  │  │  - Caching (1 hour TTL)                │        │    │
│  │  │  - Batch Processing                    │        │    │
│  │  └────────────────┬───────────────────────┘        │    │
│  │                   │                                 │    │
│  │  ┌────────────────┴───────────────────────┐        │    │
│  │  │        Retrieval Service               │        │    │
│  │  │  - Vector Search                       │        │    │
│  │  │  - Caching (30 min TTL)                │        │    │
│  │  └────────────────┬───────────────────────┘        │    │
│  └───────────────────┼────────────────────────────────┘    │
└────────────────────────┼────────────────────────────────────┘
                         │
        ┌────────────────┴────────────────┐
        │                                 │
┌───────▼────────┐              ┌─────────▼────────┐
│  PostgreSQL 16 │              │   NVIDIA NIM     │
│   + pgvector   │              │      API         │
│                │              │                  │
│  - Documents   │              │  - Embeddings    │
│  - Chunks      │              │  - Chat LLM      │
│  - Vectors     │              │                  │
│  - Metadata    │              │  Rate Limited:   │
│                │              │  1000 req/day    │
└────────────────┘              └──────────────────┘
```

---

## Data Flow Diagrams

### Document Upload Flow

```
┌─────────┐
│  User   │
└────┬────┘
     │ 1. Upload PDF/Text
     ▼
┌─────────────────┐
│ Upload          │
│ Controller      │
│ - Validate size │
│ - Check format  │
└────┬────────────┘
     │ 2. Valid file
     ▼
┌─────────────────┐
│ Processing      │
│ Service         │
│ - Extract text  │
│ - Clean content │
└────┬────────────┘
     │ 3. Raw text
     ▼
┌─────────────────┐
│ Document        │
│ Chunker         │
│ - Split text    │
│ - 800 tokens    │
│ - 100 overlap   │
└────┬────────────┘
     │ 4. Text chunks
     ▼
┌─────────────────┐
│ Embedding       │
│ Service         │
│ - Batch chunks  │
│ - Call API      │
│ - Cache results │
└────┬────────────┘
     │ 5. Generate embeddings
     ▼
┌─────────────────┐
│ NVIDIA NIM API  │
│ - nv-embed-v1   │
│ - 4096 dims     │
└────┬────────────┘
     │ 6. Vector embeddings
     ▼
┌─────────────────┐
│ Document        │
│ Repository      │
│ - Save metadata │
└────┬────────────┘
     │ 7. Save document
     ▼
┌─────────────────┐
│ Chunk           │
│ Repository      │
│ - Save chunks   │
│ - Save vectors  │
└────┬────────────┘
     │ 8. Store in DB
     ▼
┌─────────────────┐
│ PostgreSQL      │
│ + pgvector      │
└────┬────────────┘
     │ 9. Success response
     ▼
┌─────────┐
│  User   │
└─────────┘
```

### Query Processing Flow

```
┌─────────┐
│  User   │
└────┬────┘
     │ 1. Ask question
     ▼
┌─────────────────┐
│ Chat            │
│ Controller      │
│ - Validate      │
│ - Rate limit    │
└────┬────────────┘
     │ 2. Valid query
     ▼
┌─────────────────┐
│ Chat Service    │
│ - Sanitize      │
│ - Log query     │
└────┬────────────┘
     │ 3. Clean query
     ▼
┌─────────────────┐
│ Embedding       │
│ Service         │
│ - Check cache   │
│ - Generate      │
└────┬────────────┘
     │ 4. Query embedding
     ▼
┌─────────────────┐
│ Retrieval       │
│ Service         │
│ - Vector search │
│ - Apply filter  │
└────┬────────────┘
     │ 5. Search vectors
     ▼
┌─────────────────┐
│ PostgreSQL      │
│ - Cosine sim    │
│ - Top-k results │
└────┬────────────┘
     │ 6. Similar chunks
     ▼
┌─────────────────┐
│ Chat Service    │
│ - Build context │
│ - Format prompt │
└────┬────────────┘
     │ 7. Context + query
     ▼
┌─────────────────┐
│ Resilient       │
│ Chat Client     │
│ - Circuit break │
│ - Retry logic   │
└────┬────────────┘
     │ 8. Call LLM
     ▼
┌─────────────────┐
│ NVIDIA NIM API  │
│ - Llama 3.1 70B │
│ - Generate      │
└────┬────────────┘
     │ 9. AI response
     ▼
┌─────────────────┐
│ Chat Service    │
│ - Clean output  │
│ - Add sources   │
└────┬────────────┘
     │ 10. Final response
     ▼
┌─────────┐
│  User   │
└─────────┘
```

---

## Sequence Diagrams

### Upload Document Sequence

```
User    Controller  Service    Chunker   Embedding   NVIDIA   Repository   Database
 │          │          │          │          │         │          │           │
 │─Upload──>│          │          │          │         │          │           │
 │          │─Validate>│          │          │         │          │           │
 │          │<─Valid───│          │          │         │          │           │
 │          │          │─Extract─>│          │         │          │           │
 │          │          │<─Text────│          │         │          │           │
 │          │          │          │─Chunk───>│         │          │           │
 │          │          │          │<─Chunks──│         │          │           │
 │          │          │          │          │─Batch──>│          │           │
 │          │          │          │          │         │─Embed───>│           │
 │          │          │          │          │         │<─Vectors─│           │
 │          │          │          │          │<─Vectors│          │           │
 │          │          │          │          │         │          │─Save─────>│
 │          │          │          │          │         │          │<─Success──│
 │          │<─────────────────────────────────────────────────────Success────│
 │<─Success─│          │          │          │         │          │           │
```

### Query Processing Sequence

```
User    Controller  ChatSvc   Embedding  Retrieval  NVIDIA   Database
 │          │          │          │          │         │         │
 │─Query───>│          │          │          │         │         │
 │          │─Process─>│          │          │         │         │
 │          │          │─Sanitize>│          │         │         │
 │          │          │          │─Embed───>│         │         │
 │          │          │          │          │─Search─>│         │
 │          │          │          │          │         │─Query──>│
 │          │          │          │          │         │<─Chunks─│
 │          │          │          │          │<─Chunks─│         │
 │          │          │          │<─Chunks──│         │         │
 │          │          │─Context──│          │         │         │
 │          │          │          │          │         │         │
 │          │          │─Generate────────────────────>│         │
 │          │          │          │          │         │─LLM────>│
 │          │          │          │          │         │<─Answer─│
 │          │          │<─────────────────────────────Answer─────│
 │          │<─Response│          │          │         │         │
 │<─Answer──│          │          │          │         │         │
```

---

## Component Diagrams

### Service Layer Components

```
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              ChatServiceImpl                       │    │
│  │  + processQuery(String): ChatResponse              │    │
│  │  - sanitizeInput(String): String                   │    │
│  │  - constructContext(List): String                  │    │
│  │  - generateResponse(String, String): String        │    │
│  └───────┬────────────────────────────────────────────┘    │
│          │ uses                                             │
│          ▼                                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │           EmbeddingServiceImpl                     │    │
│  │  + generateQueryEmbedding(String): float[]         │    │
│  │  + generateBatchEmbeddings(List): List<float[]>    │    │
│  │  - callNvidiaAPI(String): float[]                  │    │
│  └───────┬────────────────────────────────────────────┘    │
│          │ uses                                             │
│          ▼                                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │          RetrievalServiceImpl                      │    │
│  │  + retrieveSimilarChunks(...): List<Chunk>         │    │
│  │  - convertEmbedding(float[]): String               │    │
│  │  - mapToChunk(Object[]): Chunk                     │    │
│  └───────┬────────────────────────────────────────────┘    │
│          │ uses                                             │
│          ▼                                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │       ResilientNvidiaChatClient                    │    │
│  │  + generateResponse(String, String): String        │    │
│  │  @CircuitBreaker                                   │    │
│  │  @Retry                                            │    │
│  │  @RateLimiter                                      │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Repository Layer Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         DocumentRepository                         │    │
│  │  extends JpaRepository<Document, Long>             │    │
│  │  + findByProcessingStatus(Status): List            │    │
│  │  + findByFilename(String): Optional                │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │          ChunkRepository                           │    │
│  │  extends JpaRepository<DocumentChunk, Long>        │    │
│  │  @Query("SELECT ... vector similarity ...")        │    │
│  │  + findSimilarChunks(...): List<Object[]>          │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Deployment Diagrams

### Development Deployment

```
┌─────────────────────────────────────────────────────────────┐
│                    Developer Machine                         │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Spring Boot Application                    │    │
│  │         Port: 8080                                 │    │
│  │         Profile: dev                               │    │
│  │         JVM: 2GB heap                              │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │                                       │
│                      │ JDBC                                  │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Docker Container                           │    │
│  │  ┌──────────────────────────────────────────────┐ │    │
│  │  │  PostgreSQL 16 + pgvector                    │ │    │
│  │  │  Port: 5432                                  │ │    │
│  │  │  Volume: postgres_data                       │ │    │
│  │  └──────────────────────────────────────────────┘ │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│                      │ HTTPS                                 │
│                      ▼                                       │
│              ┌───────────────┐                              │
│              │  NVIDIA NIM   │                              │
│              │     API       │                              │
│              └───────────────┘                              │
└─────────────────────────────────────────────────────────────┘
```

### Production Deployment (Docker)

```
┌─────────────────────────────────────────────────────────────┐
│                      Docker Host                             │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Nginx Reverse Proxy                        │    │
│  │         Port: 80, 443                              │    │
│  │         SSL: Let's Encrypt                         │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │                                       │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Spring Boot Container                      │    │
│  │         Port: 8080 (internal)                      │    │
│  │         Profile: prod                              │    │
│  │         JVM: 4GB heap                              │    │
│  │         Restart: always                            │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │                                       │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │         PostgreSQL Container                       │    │
│  │         Port: 5432 (internal)                      │    │
│  │         Volume: postgres_data (persistent)         │    │
│  │         Backup: Daily cron job                     │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
└───────────────────────┬──────────────────────────────────────┘
                        │ HTTPS
                        ▼
                ┌───────────────┐
                │  NVIDIA NIM   │
                │     API       │
                └───────────────┘
```

### Cloud Deployment (AWS)

```
┌─────────────────────────────────────────────────────────────┐
│                         AWS Cloud                            │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Application Load Balancer             │    │
│  │              HTTPS (ACM Certificate)               │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │                                       │
│         ┌────────────┴────────────┐                         │
│         │                         │                         │
│         ▼                         ▼                         │
│  ┌─────────────┐          ┌─────────────┐                  │
│  │   ECS Task  │          │   ECS Task  │                  │
│  │   (App 1)   │          │   (App 2)   │                  │
│  │   Fargate   │          │   Fargate   │                  │
│  └──────┬──────┘          └──────┬──────┘                  │
│         │                         │                         │
│         └────────────┬────────────┘                         │
│                      │                                       │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │              RDS PostgreSQL 16                     │    │
│  │              Multi-AZ Deployment                   │    │
│  │              Automated Backups                     │    │
│  │              Read Replicas                         │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              ElastiCache Redis                     │    │
│  │              (Distributed Cache)                   │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              CloudWatch                            │    │
│  │              Logs + Metrics + Alarms               │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
└───────────────────────┬──────────────────────────────────────┘
                        │ HTTPS
                        ▼
                ┌───────────────┐
                │  NVIDIA NIM   │
                │     API       │
                └───────────────┘
```

---

## Database Schema Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      documents                               │
├─────────────────────────────────────────────────────────────┤
│ id                BIGSERIAL PRIMARY KEY                      │
│ filename          VARCHAR(255) NOT NULL                      │
│ original_filename VARCHAR(255) NOT NULL                      │
│ file_size         BIGINT                                     │
│ upload_timestamp  TIMESTAMP NOT NULL                         │
│ status            VARCHAR(20) NOT NULL                       │
│ chunk_count       INTEGER                                    │
│ error_message     TEXT                                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ 1:N  (ON DELETE CASCADE)
                         │
┌────────────────────────┴────────────────────────────────────┐
│                   document_chunks                            │
├─────────────────────────────────────────────────────────────┤
│ id            BIGSERIAL PRIMARY KEY                          │
│ document_id   BIGINT NOT NULL REFERENCES documents(id)       │
│ chunk_number  INTEGER NOT NULL                               │
│ content       TEXT NOT NULL                                  │
│ token_count   INTEGER                                        │
│ embedding     vector(2000)  ← truncated from 4096 dims       │
│ created_at    TIMESTAMP NOT NULL                             │
├─────────────────────────────────────────────────────────────┤
│ UNIQUE (document_id, chunk_number)                          │
│ INDEX: idx_chunks_document_id ON (document_id)              │
│ INDEX: idx_documents_status   ON documents(status)          │
│ INDEX: idx_chunks_embedding                                 │
│        USING hnsw (embedding vector_cosine_ops)             │
│        WITH (m=32, ef_construction=128)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## Caching Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Cache Layers                            │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              L1: Embedding Cache                   │    │
│  │              TTL: 1 hour                           │    │
│  │              Max Size: 1000 entries                │    │
│  │              Eviction: LRU                         │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │ Cache Miss                            │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │              NVIDIA Embedding API                  │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              L2: Retrieval Cache                   │    │
│  │              TTL: 30 minutes                       │    │
│  │              Max Size: 500 entries                 │    │
│  │              Eviction: LRU                         │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │ Cache Miss                            │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │              PostgreSQL Vector Search              │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Resilience Patterns

```
┌─────────────────────────────────────────────────────────────┐
│                  Resilience Layers                           │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Rate Limiter                          │    │
│  │              20 requests/second                    │    │
│  │              Burst: 40 requests                    │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │ Allowed                               │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Circuit Breaker                       │    │
│  │              Failure Rate: 50%                     │    │
│  │              Wait Duration: 30s                    │    │
│  │              States: CLOSED → OPEN → HALF_OPEN     │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │ Closed                                │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Retry Logic                           │    │
│  │              Max Attempts: 2                       │    │
│  │              Wait: 1s (exponential backoff)        │    │
│  └───────────────────┬────────────────────────────────┘    │
│                      │ Success                               │
│                      ▼                                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │              NVIDIA API Call                       │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

These diagrams provide visual representations of the system architecture, data flows, and component interactions. For more detailed information, refer to the [Architecture Guide](ARCHITECTURE.md).
