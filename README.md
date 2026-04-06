# Intelligent Memory Preservation & Retrieval System (IMPRS)

Enterprise-grade AI-powered platform utilizing advanced RAG architecture, pgvector semantic search, NVIDIA NIM embeddings, and LLM-based retrieval to help users preserve, organize, and recall their personal memories and life experiences.

## Features

- **Memory Sharing**: Upload and store precious memories and personal stories
- **Semantic Search**: Find memories using natural language queries with pgvector
- **AI Chat**: Conversational interface to explore and recall memories
- **Document Processing**: Automatic chunking and embedding of text content
- **Real-time Status**: Track document processing status
- **RESTful API**: Complete API with Swagger documentation
- **Container Reuse**: Automatically reuses existing Docker containers to preserve data
- **Auto Schema Init**: Database schema is automatically initialized on startup

## Tech Stack

- **Backend**: Spring Boot 3.x, Java 17
- **Database**: PostgreSQL 16 with pgvector extension (Docker)
- **AI/ML**: NVIDIA NIM API
  - Embedding: `nvidia/nv-embed-v1` (4096 dims, truncated to 2000)
  - Chat: `nvidia/nemotron-3-super-120b-a12b`
- **Vector Search**: pgvector with HNSW indexing
- **API Documentation**: OpenAPI 3.0 / Swagger UI

## Quick Start

### Prerequisites

- Docker Desktop (running)
- Java 17+
- Maven (included via mvnw)
- NVIDIA API Key (get from https://build.nvidia.com)

### Setup Steps

1. **Check Prerequisites**
   ```bash
   1_CHECK_PREREQUISITES.bat
   ```

2. **Configure API Key**
   ```bash
   2_CONFIGURE_API_KEY.bat
   ```
   Enter your NVIDIA API key when prompted.

3. **Start Application**
   ```bash
   3_START_APPLICATION.bat
   ```
   This will:
   - Start or reuse existing PostgreSQL container
   - Initialize database schema if needed
   - Start the Spring Boot application

4. **Access Swagger UI**
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

### Stopping

```bash
STOP_DOCKER_DB.bat
```

## API Endpoints

### Upload Memory (Text)
```
POST /api/documents/text
Content-Type: application/json

{
  "content": "Your memory text here"
}
```

### Chat/Query
```
POST /api/chat
Content-Type: application/json

{
  "query": "What do you remember about my grandchildren?"
}
```

### List Documents
```
GET /api/documents
```

### Health Check
```
GET /api/health
```

## Architecture

```
User Request
    ↓
ChatController
    ↓
ChatService (orchestration)
    ↓
├─→ EmbeddingService → NvidiaEmbeddingClient (4096 dims → truncate to 2000)
├─→ RetrievalService → ChunkRepository (pgvector similarity search)
└─→ ResilientNvidiaChatClient (circuit breaker + retry)
    ↓
Response with sources
```

## Configuration

### NVIDIA Models

**Embedding Model**: `nvidia/nv-embed-v1`
- Outputs 4096-dimensional embeddings
- Automatically truncated to 2000 dimensions (pgvector HNSW limit)
- Supports `query` and `passage` input types

**Chat Model**: `nvidia/nemotron-3-super-120b-a12b`

To change models, edit `demo/src/main/resources/application.properties`:
```properties
nvidia.api.embedding-model=nvidia/nv-embed-v1
nvidia.api.chat-model=nvidia/nemotron-3-super-120b-a12b
```

### Vector Database
- PostgreSQL 16 with pgvector extension
- HNSW index for O(log n) similarity search
- 2000-dimensional vectors (pgvector limit)
- Cosine distance metric

### Retrieval Settings
```properties
app.retrieval.top-k=5                    # Number of results
app.retrieval.similarity-threshold=0.3   # Minimum similarity (0.0-1.0)
app.retrieval.diversity-threshold=0.7    # Diversity filtering
```

### Chunking Strategy
```properties
app.chunking.chunk-size=800    # Characters per chunk
app.chunking.overlap=100       # Overlap between chunks
```

## Database Schema

### documents
- `id` - Primary key
- `filename` - Document name
- `status` - Processing status (PENDING, PROCESSING, COMPLETED, FAILED)
- `chunk_count` - Number of chunks
- `upload_timestamp` - Upload time

### document_chunks
- `id` - Primary key
- `document_id` - Foreign key to documents
- `chunk_number` - Chunk sequence
- `content` - Text content
- `embedding` - Vector embedding (2000 dimensions)
- HNSW index on `embedding` for fast similarity search

## Project Structure

```
.
├── demo/                          # Spring Boot application
│   ├── src/main/java/            # Java source code
│   │   └── com/example/demo/
│   │       ├── config/           # Configuration classes
│   │       ├── controller/       # REST controllers
│   │       ├── dto/              # Data transfer objects
│   │       ├── exception/        # Exception handlers
│   │       ├── model/            # JPA entities
│   │       ├── repository/       # Data repositories
│   │       └── service/          # Business logic
│   ├── src/main/resources/       # Configuration files
│   │   ├── application.properties
│   │   └── schema.sql
│   └── pom.xml                   # Maven dependencies
├── docker-compose.yml            # Docker configuration
├── 1_CHECK_PREREQUISITES.bat     # Check system requirements
├── 2_CONFIGURE_API_KEY.bat       # Configure NVIDIA API key
├── 3_START_APPLICATION.bat       # Start everything
├── 4_OPEN_SWAGGER_UI.bat         # Open Swagger UI
├── STOP_DOCKER_DB.bat            # Stop database
└── README.md                     # This file
```

## Technical Details

### Embedding Truncation
The `nvidia/nv-embed-v1` model outputs 4096-dimensional embeddings, but pgvector's HNSW index has a 2000-dimension limit. The application automatically truncates embeddings to the first 2000 dimensions while maintaining good semantic quality.

### Container Management
The startup script intelligently handles Docker containers:
- If container exists and is running → uses it as-is
- If container exists but is stopped → starts it
- If container doesn't exist → creates a new one

This preserves your data between restarts.

### Schema Initialization
On every startup, the script checks if the database schema exists and initializes it if needed. This ensures the database is always ready.

## Development

### Run Tests
```bash
cd demo
mvnw test
```

### Build
```bash
cd demo
mvnw clean package
```

### Run with Profile
```bash
cd demo
mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Troubleshooting

### Container Already Exists
The startup script automatically handles this - it will reuse the existing container.

### Database Schema Missing
The startup script checks and initializes the schema automatically.

### Embedding API Errors
- Verify your NVIDIA API key is valid
- Check API rate limits at https://build.nvidia.com
- Review application logs for detailed error messages

### No Results from Queries
- Ensure documents are uploaded successfully (status: COMPLETED)
- Check that embeddings are being generated
- Verify the similarity threshold (currently 0.3)

### Port Already in Use
```
Error: Port 5432 is already allocated
Solution: Stop other PostgreSQL instances or change port in docker-compose.yml
```

## Performance

### Retrieval Speed
- HNSW index provides O(log n) search complexity
- Typical query time: <100ms for 1000s of chunks

### Embedding Generation
- Batch processing for efficiency
- Retry logic with exponential backoff
- Circuit breaker to prevent cascading failures

## Security

- API keys must be configured via environment variables (never commit to git)
- Use `.env.example` as a template for required environment variables
- Input sanitization to prevent prompt injection
- Rate limiting on NVIDIA API calls
- Circuit breaker to prevent cascading failures
- CORS configuration should be updated for production domains

## License

MIT License

## Support

For issues and questions, please open an issue on GitHub.
