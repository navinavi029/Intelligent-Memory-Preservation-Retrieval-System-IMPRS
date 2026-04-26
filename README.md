# Intelligent Memory Preservation & Retrieval System (IMPRS)

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![pgvector](https://img.shields.io/badge/pgvector-enabled-purple.svg)](https://github.com/pgvector/pgvector)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Overview

IMPRS is an enterprise-grade AI-powered memory preservation platform that leverages advanced RAG (Retrieval-Augmented Generation) architecture, pgvector semantic search, and NVIDIA NIM embeddings to help Alzheimer's and dementia patients preserve, organize, and recall their personal memories and life experiences.

### Key Features

- **🧠 Semantic Memory Search**: Vector-based similarity search using pgvector for accurate memory retrieval
- **📄 Multi-Format Support**: Process PDFs and text documents with intelligent chunking
- **💬 Conversational AI**: Natural language chat interface powered by NVIDIA LLM models
- **🔒 Enterprise Security**: Spring Security integration with input sanitization and validation
- **⚡ High Performance**: Optimized with caching, connection pooling, and async processing
- **🛡️ Resilience**: Circuit breakers, retry logic, and rate limiting via Resilience4j
- **📊 Observability**: Prometheus metrics and Spring Boot Actuator endpoints
- **🎯 Production-Ready**: Comprehensive error handling and logging

## Architecture

### Technology Stack

- **Backend Framework**: Spring Boot 3.5.12
- **Language**: Java 17
- **Database**: PostgreSQL 16 with pgvector extension
- **AI/ML**: 
  - NVIDIA NIM API for embeddings (`nvidia/nv-embed-v1`)
  - NVIDIA LLM for chat (`meta/llama-3.1-70b-instruct`)
  - Spring AI integration
- **Resilience**: Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- **Caching**: Caffeine Cache
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Build Tool**: Maven

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Application                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   REST API Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Chat       │  │   Upload     │  │   Health     │     │
│  │ Controller   │  │ Controller   │  │ Controller   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   Service Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Chat       │  │  Embedding   │  │  Retrieval   │     │
│  │  Service     │  │   Service    │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │   PDF        │  │    Text      │                        │
│  │ Processing   │  │ Processing   │                        │
│  └──────────────┘  └──────────────┘                        │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────────┐    ┌──────────────────┐
│  PostgreSQL +    │    │   NVIDIA NIM     │
│    pgvector      │    │      API         │
│                  │    │                  │
│  - Documents     │    │  - Embeddings    │
│  - Chunks        │    │  - Chat LLM      │
│  - Vectors       │    │                  │
└──────────────────┘    └──────────────────┘
```

### RAG Pipeline Flow

1. **Document Ingestion**
   - Upload PDF or text documents
   - Extract and clean text content
   - Split into semantic chunks (800 tokens with 100 token overlap)

2. **Embedding Generation**
   - Generate vector embeddings using NVIDIA NIM API
   - Store embeddings in PostgreSQL with pgvector

3. **Query Processing**
   - User submits natural language query
   - Generate query embedding
   - Perform vector similarity search (cosine similarity)
   - Retrieve top-k relevant chunks

4. **Response Generation**
   - Construct context window from retrieved chunks
   - Send to NVIDIA LLM with system prompt
   - Generate caring, contextual response
   - Return answer with source citations

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- NVIDIA API Key ([Get one here](https://build.nvidia.com))

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd IMPRS
```

2. **Set up environment variables**
```bash
cp .env.example .env
# Edit .env and add your NVIDIA API key
```

3. **Start PostgreSQL with pgvector**
```bash
docker-compose up -d
```

4. **Build the application**
```bash
cd demo
./mvnw clean install
```

5. **Run the application**
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### Quick Start with Windows

For Windows users, convenience scripts are provided:

```bash
# Start the database
SETUP.bat

# Stop the database
STOP_DOCKER_DB.bat
```

## API Documentation

### Swagger UI

Access the interactive API documentation at:
```
http://localhost:8080/swagger-ui/index.html
```

### Key Endpoints

#### Share a Memory (Text)
```http
POST /api/documents
Content-Type: application/json

{
  "memory": "Today I went to the beach with my family..."
}
```

#### List All Documents
```http
GET /api/documents
```

#### Get Document Status
```http
GET /api/documents/{id}/status
```

#### Delete a Document
```http
DELETE /api/documents/{id}
```

#### Chat Query
```http
POST /api/chat/query
Content-Type: application/json

{
  "query": "Tell me about my vacation memories"
}
```

Response:
```json
{
  "answer": "Based on your shared memories, you had a wonderful vacation...",
  "sources": [
    {
      "documentId": 123,
      "filename": "vacation-2024.pdf",
      "chunkNumber": 5,
      "similarityScore": 0.85
    }
  ],
  "retrievedChunks": 2
}
```

#### Health Check
```http
GET /api/health
```

## Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# NVIDIA API Configuration
nvidia.api.key=${NVIDIA_API_KEY}
nvidia.api.embedding-model=nvidia/nv-embed-v1
nvidia.api.chat-model=meta/llama-3.1-70b-instruct

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/ragdb
spring.datasource.username=raguser
spring.datasource.password=ragpass

# Chunking Configuration
app.chunking.chunk-size=800
app.chunking.overlap=100

# Retrieval Configuration
app.retrieval.top-k=8
app.retrieval.similarity-threshold=0.25

# Resilience Configuration
resilience4j.circuitbreaker.instances.nvidia-api.failure-rate-threshold=50
resilience4j.retry.instances.nvidia-api.max-attempts=2
resilience4j.ratelimiter.instances.nvidia-api.limit-for-period=20
```

### Environment Profiles

- `dev`: Development profile with debug logging
- `local`: Local development with relaxed security
- `prod`: Production profile with optimized settings

Activate a profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Database Schema

### Documents Table
```sql
CREATE TABLE documents (
    id                BIGSERIAL PRIMARY KEY,
    filename          VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size         BIGINT,
    upload_timestamp  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status            VARCHAR(20) NOT NULL,
    chunk_count       INTEGER,
    error_message     TEXT
);
```

### Document Chunks Table
```sql
-- Note: embeddings are truncated to 2000 dimensions from the full 4096
-- to stay within pgvector's HNSW index limits
CREATE TABLE document_chunks (
    id            BIGSERIAL PRIMARY KEY,
    document_id   BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_number  INTEGER NOT NULL,
    content       TEXT NOT NULL,
    token_count   INTEGER,
    embedding     vector(2000),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_document_chunk UNIQUE (document_id, chunk_number)
);

-- HNSW index for fast approximate nearest-neighbor search (cosine distance)
CREATE INDEX idx_chunks_embedding ON document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);

-- Supporting indexes
CREATE INDEX idx_documents_status   ON documents(status);
CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
```

## Performance Optimization

### Vector Search Optimization

The system uses IVFFlat indexing for fast approximate nearest neighbor search:

```sql
-- Create optimized index
CREATE INDEX idx_chunks_embedding ON document_chunks 
    USING ivfflat (embedding vector_cosine_ops) 
    WITH (lists = 100);

-- Analyze for query planning
ANALYZE document_chunks;
```

### Caching Strategy

- **Embedding Cache**: Caches generated embeddings (TTL: 1 hour)
- **Retrieval Cache**: Caches search results (TTL: 30 minutes)
- **Connection Pool**: HikariCP with 20 max connections

### Async Processing

Document processing runs asynchronously to avoid blocking API responses:

```java
@Async
public CompletableFuture<ProcessingResult> processDocument(MultipartFile file)
```

## Security

### Input Validation

- Request size limits (10MB max)
- Content length validation
- Input sanitization to prevent prompt injection
- SQL injection prevention via JPA

### API Security

- Spring Security integration
- CORS configuration
- Rate limiting (20 requests/second per client)
- Circuit breaker protection

### Data Protection

- Secure credential management via environment variables
- Database connection encryption
- Audit logging for sensitive operations

## Monitoring & Observability

### Actuator Endpoints

```
GET /actuator/health       - Health status
GET /actuator/metrics      - Application metrics
GET /actuator/prometheus   - Prometheus metrics
```

### Metrics

- Request latency (p50, p95, p99)
- Error rates
- Circuit breaker states
- Cache hit rates
- Database connection pool stats

### Logging

Structured logging with correlation IDs:

```
[ChatService] Query processing completed - retrievedChunks: 5, sources: 3, answerLength: 245
```

## Troubleshooting

### Common Issues

**Database Connection Failed**
```bash
# Check if PostgreSQL is running
docker ps | grep rag-postgres

# Restart the database
docker-compose restart postgres
```

**NVIDIA API Errors**
```bash
# Verify API key is set
echo $NVIDIA_API_KEY

# Check API status
curl -H "Authorization: Bearer $NVIDIA_API_KEY" \
     https://integrate.api.nvidia.com/v1/models
```

**Out of Memory**
```bash
# Increase JVM heap size
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g"
```

### Debug Mode

Enable debug logging:
```properties
logging.level.com.example.demo=DEBUG
logging.level.org.springframework.ai=DEBUG
```

## Development

### Project Structure

```
demo/
├── src/main/java/com/example/demo/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── dto/            # Data transfer objects
│   ├── exception/      # Exception handlers
│   ├── model/          # JPA entities
│   ├── repository/     # Data access layer
│   └── service/        # Business logic
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-prod.properties
│   └── schema.sql
└── pom.xml
```

### Building for Production

```bash
# Build JAR
./mvnw clean package -DskipTests

# Run production build
java -jar target/intelligent-memory-preservation-system-1.0.0.jar \
     --spring.profiles.active=prod
```

### Running Tests

```bash
./mvnw test
```

## Deployment

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build and run:
```bash
docker build -t imprs:latest .
docker run -p 8080:8080 --env-file .env imprs:latest
```

### Environment Variables

Required environment variables for production:

```bash
NVIDIA_API_KEY=your-api-key
DATABASE_URL=jdbc:postgresql://db-host:5432/ragdb
DB_USERNAME=raguser
DB_PASSWORD=secure-password
SPRING_PROFILES_ACTIVE=prod
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- NVIDIA for providing the NIM API
- pgvector team for the excellent PostgreSQL extension
- Spring AI team for the AI integration framework
- The open-source community

## Documentation

### Getting Started
- **[Quick Start Guide](docs/QUICK_START.md)** - Get running in 5 minutes
- **[Installation Guide](#installation)** - Detailed setup instructions
- **[Configuration Guide](#configuration)** - Configure the application

### API Documentation
- **[API Reference](docs/API_REFERENCE.md)** - Complete API documentation
- **[Swagger UI](http://localhost:8080/swagger-ui/index.html)** - Interactive API explorer (when running)

### Architecture & Development
- **[Architecture Guide](docs/ARCHITECTURE.md)** - System design and components
- **[Development Guide](docs/DEVELOPMENT.md)** - Developer setup and guidelines
- **[Deployment Guide](docs/DEPLOYMENT.md)** - Production deployment instructions

### Additional Resources
- **[FAQ](docs/FAQ.md)** - Frequently asked questions
- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute
- **[Changelog](CHANGELOG.md)** - Version history and changes

## Support

For issues and questions:
- **Quick Help**: Check the [FAQ](docs/FAQ.md)
- **Bug Reports**: Create an issue on GitHub
- **Documentation**: Browse the [docs/](docs/) directory
- **Community**: Join our discussions

## Roadmap

- [ ] Multi-user support with authentication
- [ ] Image and video memory support
- [ ] Mobile application
- [ ] Voice interface
- [ ] Advanced analytics dashboard
- [ ] Export memories to PDF
- [ ] Family sharing features

---

**Built with ❤️ for those who cherish memories**
