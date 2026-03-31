# API Reference

## Base URL
```
http://localhost:8080
```

## Document Management

### Upload Document
Upload a PDF document for processing.

**Endpoint**: `POST /api/documents`

**Content-Type**: `multipart/form-data`

**Request**:
```bash
curl -X POST http://localhost:8080/api/documents \
  -F "file=@document.pdf"
```

**Response**: `202 Accepted`
```json
{
  "documentId": 123,
  "filename": "document_20260331_143022.pdf",
  "originalFilename": "document.pdf",
  "status": "PENDING",
  "uploadTimestamp": "2026-03-31T14:30:22.123Z"
}
```

**Errors**:
- `400 Bad Request`: Invalid file type or size
- `413 Payload Too Large`: File exceeds 10MB
- `500 Internal Server Error`: Processing error

**Implementation**: `UploadController.java:55`

---

### Check Processing Status
Poll document processing status.

**Endpoint**: `GET /api/documents/{documentId}/status`

**Request**:
```bash
curl http://localhost:8080/api/documents/123/status
```

**Response**: `200 OK`
```json
{
  "documentId": 123,
  "status": "COMPLETED",
  "chunksProcessed": 42,
  "errorMessage": null
}
```

**Status Values**:
- `PENDING`: Queued for processing
- `PROCESSING`: Currently being processed
- `COMPLETED`: Successfully processed
- `FAILED`: Processing failed (see errorMessage)

**Implementation**: `UploadController.java:213`

---

### List Documents
Get all uploaded documents.

**Endpoint**: `GET /api/documents`

**Request**:
```bash
curl http://localhost:8080/api/documents
```

**Response**: `200 OK`
```json
[
  {
    "documentId": 123,
    "filename": "document_20260331_143022.pdf",
    "originalFilename": "document.pdf",
    "fileSize": 1048576,
    "uploadTimestamp": "2026-03-31T14:30:22.123Z",
    "status": "COMPLETED",
    "chunkCount": 42
  }
]
```

**Implementation**: `UploadController.java:213`

---

### Get Document Metadata
Get metadata for a specific document.

**Endpoint**: `GET /api/documents/{documentId}`

**Request**:
```bash
curl http://localhost:8080/api/documents/123
```

**Response**: `200 OK`
```json
{
  "documentId": 123,
  "filename": "document_20260331_143022.pdf",
  "originalFilename": "document.pdf",
  "fileSize": 1048576,
  "uploadTimestamp": "2026-03-31T14:30:22.123Z",
  "status": "COMPLETED",
  "chunkCount": 42,
  "errorMessage": null
}
```

**Errors**:
- `404 Not Found`: Document doesn't exist

**Implementation**: `UploadController.java:271`

---

### Delete Document
Delete a document and all its chunks.

**Endpoint**: `DELETE /api/documents/{documentId}`

**Request**:
```bash
curl -X DELETE http://localhost:8080/api/documents/123
```

**Response**: `204 No Content`

**Errors**:
- `404 Not Found`: Document doesn't exist

**Implementation**: `UploadController.java:342`

---

## Chat

### Query with RAG
Ask a question using Retrieval-Augmented Generation.

**Endpoint**: `POST /api/chat/query`

**Content-Type**: `application/json`

**Request**:
```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is machine learning?"
  }'
```

**Response**: `200 OK`
```json
{
  "response": "Machine learning is a subset of artificial intelligence...",
  "sources": [
    {
      "documentId": 123,
      "filename": "ml_guide.pdf",
      "chunkNumber": 5,
      "similarity": 0.87
    },
    {
      "documentId": 123,
      "filename": "ml_guide.pdf",
      "chunkNumber": 12,
      "similarity": 0.82
    }
  ],
  "timestamp": "2026-03-31T14:35:10.456Z"
}
```

**Errors**:
- `400 Bad Request`: Empty or invalid query
- `429 Too Many Requests`: Rate limit exceeded
- `503 Service Unavailable`: Circuit breaker open (NVIDIA API down)

**Implementation**: `ChatController.java:43`

---

## Monitoring

### Health Check
Check application health status.

**Endpoint**: `GET /actuator/health`

**Request**:
```bash
curl http://localhost:8080/actuator/health
```

**Response**: `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

### Application Metrics
Get application metrics.

**Endpoint**: `GET /actuator/metrics`

**Request**:
```bash
curl http://localhost:8080/actuator/metrics
```

**Response**: `200 OK`
```json
{
  "names": [
    "jvm.memory.used",
    "http.server.requests",
    "resilience4j.circuitbreaker.state",
    "cache.gets",
    "executor.active"
  ]
}
```

**Get specific metric**:
```bash
curl http://localhost:8080/actuator/metrics/cache.gets
```

---

### Prometheus Metrics
Get metrics in Prometheus format.

**Endpoint**: `GET /actuator/prometheus`

**Request**:
```bash
curl http://localhost:8080/actuator/prometheus
```

**Response**: `200 OK` (Prometheus text format)
```
# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="POST",uri="/api/chat/query",status="200"} 1234
http_server_requests_seconds_sum{method="POST",uri="/api/chat/query",status="200"} 45.67
```

---

## Error Responses

All errors follow this format:

```json
{
  "timestamp": "2026-03-31T14:40:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Query cannot be empty",
  "path": "/api/chat/query"
}
```

**Common Status Codes**:
- `400 Bad Request`: Invalid input
- `404 Not Found`: Resource doesn't exist
- `413 Payload Too Large`: File too large
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error
- `503 Service Unavailable`: Circuit breaker open

**Implementation**: `GlobalExceptionHandler.java`

---

## Rate Limiting

**Limits**:
- Chat queries: 100 requests/minute per IP
- Document uploads: No specific limit (controlled by async processing)

**Rate Limit Headers**:
```
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1711896000
```

**Implementation**: `SecurityConfig.java`

---

## Configuration

### Tuning Parameters

**Chunking** (`application.properties`):
```properties
app.chunking.chunk-size=500      # Tokens per chunk
app.chunking.overlap=50          # Overlap between chunks
```

**Retrieval** (`application.properties`):
```properties
app.retrieval.top-k=5                    # Number of chunks to retrieve
app.retrieval.similarity-threshold=0.3   # Minimum similarity score
```

**Circuit Breaker** (`application.properties`):
```properties
resilience4j.circuitbreaker.instances.nvidia-api.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.nvidia-api.wait-duration-in-open-state=30s
```

---

## OpenAPI/Swagger

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui/index.html
```

**References**:
- Controllers: `demo/src/main/java/com/example/demo/controller/`
- DTOs: `demo/src/main/java/com/example/demo/dto/`
- Config: `demo/src/main/resources/application.properties:1`
