# Project Overview

## What is PDF RAG Chatbot?

A production-ready Retrieval-Augmented Generation (RAG) system that transforms PDF documents into an intelligent, queryable knowledge base using vector similarity search and large language models.

## Core Capabilities

1. **Document Processing**
   - Upload PDF documents (up to 10MB)
   - Automatic text extraction using Apache PDFBox
   - Intelligent chunking with configurable size and overlap
   - Asynchronous processing (no HTTP timeouts)

2. **Vector Embeddings**
   - Generate 2048-dimensional embeddings using NVIDIA NIM
   - Batch processing for efficiency
   - Intelligent caching to reduce API costs

3. **Semantic Search**
   - PostgreSQL with pgvector extension
   - Cosine similarity search
   - IVFFlat indexing for fast retrieval
   - Configurable top-k results

4. **AI-Powered Responses**
   - Context-aware responses using NVIDIA Llama 3.1
   - Source attribution with chunk references
   - Circuit breaker protection
   - Automatic retry with exponential backoff

5. **Production Features**
   - Async document processing
   - Multi-level caching (Caffeine)
   - Circuit breaker pattern (Resilience4j)
   - Rate limiting (Bucket4j)
   - Security headers and CORS
   - Health checks and metrics (Actuator)
   - Prometheus integration

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.12
- **Language**: Java 17
- **Build Tool**: Maven

### AI/ML
- **LLM Provider**: NVIDIA NIM
- **Embedding Model**: llama-3.2-nemoretriever-300m-embed-v1 (2048 dims)
- **Chat Model**: meta/llama-3.1-8b-instruct
- **Spring AI**: 1.1.3

### Database
- **RDBMS**: PostgreSQL 14+
- **Vector Extension**: pgvector
- **ORM**: Spring Data JPA / Hibernate

### Resilience & Performance
- **Circuit Breaker**: Resilience4j
- **Caching**: Caffeine
- **Rate Limiting**: Bucket4j
- **Async Processing**: Spring @Async

### Monitoring
- **Metrics**: Micrometer + Prometheus
- **Health Checks**: Spring Boot Actuator
- **API Docs**: SpringDoc OpenAPI

## Architecture Highlights

### Async Processing Pattern
```
Upload → 202 Accepted (immediate) → Background Processing → Poll Status
```
No HTTP timeouts, better resource utilization.

### Resilience Patterns
- **Circuit Breaker**: Fails fast when NVIDIA API is down
- **Retry**: Exponential backoff for transient failures
- **Rate Limiter**: Protects against burst traffic
- **Caching**: 60-80% cost reduction

### Security Layers
- Spring Security with security headers
- Restricted CORS (configurable origins)
- Rate limiting (100 req/min per IP)
- Input sanitization
- CSRF protection (configurable)

## Use Cases

1. **Enterprise Knowledge Base**
   - Internal documentation search
   - Policy and procedure queries
   - Training material Q&A

2. **Research Assistant**
   - Academic paper analysis
   - Literature review support
   - Citation extraction

3. **Legal Document Analysis**
   - Contract review
   - Case law research
   - Compliance checking

4. **Customer Support**
   - Product manual queries
   - Troubleshooting guides
   - FAQ automation

## Performance Characteristics

| Metric | Value |
|--------|-------|
| Upload Response | < 100ms (async) |
| Cached Query | < 10ms |
| Uncached Query | 2-3 seconds |
| Document Processing | 30-120 seconds (background) |
| Max File Size | 10MB |
| Chunk Size | 500 tokens (configurable) |
| Embedding Dimensions | 2048 |
| Top-K Results | 5 (configurable) |

## API Endpoints

### Document Management
- `POST /api/documents` - Upload PDF (returns 202 Accepted)
- `GET /api/documents/{id}/status` - Check processing status
- `GET /api/documents` - List all documents
- `GET /api/documents/{id}` - Get document metadata
- `DELETE /api/documents/{id}` - Delete document

### Chat
- `POST /api/chat/query` - Query with RAG

### Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Project Status

**Current Phase**: Production-Ready (Phase 1)
- ✅ Async processing
- ✅ Circuit breaker
- ✅ Caching
- ✅ Security
- ✅ Monitoring

**Next Phase**: Scaling (Phase 2)
- [ ] Redis distributed cache
- [ ] Message queue (RabbitMQ/Kafka)
- [ ] JWT authentication
- [ ] Database read replicas

**References**:
- Architecture: `demo/docs/ARCHITECTURE_IMPROVEMENTS.md`
- ADR: `demo/docs/ADR-001-async-processing-and-resilience.md`
- POM: `demo/pom.xml:1`
