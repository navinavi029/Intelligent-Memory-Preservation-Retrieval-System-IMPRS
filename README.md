# PDF RAG Chatbot

A Retrieval-Augmented Generation (RAG) chatbot application built with Spring Boot that allows users to upload PDF documents and ask questions about their content using NVIDIA's AI models.

## Features

- PDF document upload and processing
- Automatic text chunking with configurable overlap
- Vector embeddings using NVIDIA's NeMo Retriever model
- Semantic search with PostgreSQL + pgvector
- Question answering using NVIDIA's Llama 3.1 model
- RESTful API with Swagger documentation
- Web-based testing interface

## Technology Stack

- **Backend**: Spring Boot 3.5.12, Java 17
- **AI/ML**: Spring AI 1.1.3, NVIDIA NIM API
- **Database**: PostgreSQL with pgvector extension
- **Vector Search**: IVFFlat indexing for 2048-dimensional embeddings
- **API Documentation**: SpringDoc OpenAPI (Swagger)

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher
- NVIDIA API key (free tier available)

## Quick Start

See [HOW_TO_RUN.md](HOW_TO_RUN.md) for detailed setup and running instructions.

## Project Structure

```
demo/
├── src/main/java/com/example/demo/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST API endpoints
│   ├── dto/            # Data transfer objects
│   ├── exception/      # Exception handling
│   ├── model/          # JPA entities
│   ├── repository/     # Data access layer
│   └── service/        # Business logic
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.properties
│   └── schema.sql
└── pom.xml
```

## API Endpoints

### Upload Document
```
POST /api/documents
Content-Type: multipart/form-data
Body: file (PDF)
```

### Chat Query
```
POST /api/chat/query
Content-Type: application/json
Body: { "query": "your question here" }
```

### API Documentation
- Swagger UI: http://localhost:8080/swagger-ui

## Configuration

Key configuration properties in `application.properties`:

- `nvidia.api.key`: Your NVIDIA API key
- `spring.datasource.url`: PostgreSQL connection URL
- `app.chunking.chunk-size`: Text chunk size (default: 500)
- `app.retrieval.top-k`: Number of chunks to retrieve (default: 5)

## Testing

Use the included `test-rag-api-v2.html` file to test the API:
1. Open the file in a web browser
2. Upload a PDF document
3. Ask questions about the document

## License

This project is provided as-is for educational and demonstration purposes.
