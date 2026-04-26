# API Reference

Complete API documentation for the Intelligent Memory Preservation & Retrieval System.

## Base URL

```
http://localhost:8080/api
```

## Authentication

Currently, the API does not require authentication. Future versions will implement JWT-based authentication.

## Endpoints

### 1. Upload PDF Document

Upload a PDF document for processing and embedding generation.

**Endpoint:** `POST /upload/pdf`

**Content-Type:** `multipart/form-data`

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | File | Yes | PDF file to upload (max 10MB) |

**Example Request:**

```bash
curl -X POST http://localhost:8080/api/upload/pdf \
  -F "file=@/path/to/document.pdf"
```

**Success Response (200 OK):**

```json
{
  "documentId": 123,
  "filename": "document.pdf",
  "fileSize": 1048576,
  "totalChunks": 15,
  "processingStatus": "COMPLETED",
  "uploadDate": "2024-01-15T10:30:00"
}
```

**Error Responses:**

- `400 Bad Request`: Invalid file format or size exceeded
- `500 Internal Server Error`: Processing failed

---

### 2. Upload Text Document

Upload text content directly without a file.

**Endpoint:** `POST /upload/text`

**Content-Type:** `application/json`

**Request Body:**

```json
{
  "content": "Your text content here. This can be a diary entry, note, or any text you want to preserve.",
  "filename": "my-memory.txt"
}
```

**Field Descriptions:**

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| content | String | Yes | 10,000 chars | The text content to store |
| filename | String | Yes | 255 chars | Name for the document |

**Example Request:**

```bash
curl -X POST http://localhost:8080/api/upload/text \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Today was a beautiful day at the beach...",
    "filename": "beach-memory.txt"
  }'
```

**Success Response (200 OK):**

```json
{
  "documentId": 124,
  "filename": "beach-memory.txt",
  "fileSize": 256,
  "totalChunks": 1,
  "processingStatus": "COMPLETED",
  "uploadDate": "2024-01-15T10:35:00"
}
```

**Error Responses:**

- `400 Bad Request`: Missing required fields or content too long
- `500 Internal Server Error`: Processing failed

---

### 3. Chat Query

Ask questions about your uploaded memories and receive AI-generated responses.

**Endpoint:** `POST /chat/query`

**Content-Type:** `application/json`

**Request Body:**

```json
{
  "query": "Tell me about my beach memories"
}
```

**Field Descriptions:**

| Field | Type | Required | Max Length | Description |
|-------|------|----------|------------|-------------|
| query | String | Yes | 1,000 chars | Your question about memories |

**Example Request:**

```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What did I do on my vacation?"
  }'
```

**Success Response (200 OK):**

```json
{
  "answer": "Based on your shared memories, you had a wonderful vacation at the beach. You mentioned enjoying the sunset and collecting seashells with your family.",
  "sources": [
    {
      "documentId": 124,
      "filename": "beach-memory.txt",
      "chunkNumber": 1,
      "similarityScore": 0.87
    },
    {
      "documentId": 125,
      "filename": "vacation-2024.pdf",
      "chunkNumber": 3,
      "similarityScore": 0.82
    }
  ],
  "retrievedChunks": 2
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| answer | String | AI-generated response based on retrieved memories |
| sources | Array | List of source documents used to generate the answer |
| retrievedChunks | Integer | Number of relevant memory chunks found |

**Source Object:**

| Field | Type | Description |
|-------|------|-------------|
| documentId | Long | ID of the source document |
| filename | String | Name of the source file |
| chunkNumber | Integer | Chunk number within the document |
| similarityScore | Double | Similarity score (0.0 to 1.0) |

**Special Responses:**

When no relevant memories are found:
```json
{
  "answer": "I don't have that memory yet, but I'd love to hear about it if you'd like to share!",
  "sources": [],
  "retrievedChunks": 0
}
```

**Error Responses:**

- `400 Bad Request`: Empty or invalid query
- `500 Internal Server Error`: Processing failed

---

### 4. List All Documents

Retrieve all uploaded documents ordered by most recent first.

**Endpoint:** `GET /documents`

**Example Request:**

```bash
curl http://localhost:8080/api/documents
```

**Success Response (200 OK):**

```json
[
  {
    "id": 123,
    "filename": "vacation-2024.pdf",
    "originalFilename": "vacation-2024.pdf",
    "fileSize": 2048576,
    "uploadTimestamp": "2024-01-15T10:30:00",
    "status": "COMPLETED",
    "chunkCount": 25,
    "errorMessage": null
  },
  {
    "id": 122,
    "filename": "Had a wonderful visit with my grandchildren today...",
    "originalFilename": "Had a wonderful visit with my grandchildren today...",
    "fileSize": 156,
    "uploadTimestamp": "2024-01-15T09:15:00",
    "status": "COMPLETED",
    "chunkCount": 1,
    "errorMessage": null
  }
]
```

---

### 5. Get Document Details

Retrieve metadata for a specific document by ID.

**Endpoint:** `GET /documents/{documentId}`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| documentId | Long | Unique document identifier |

**Example Request:**

```bash
curl http://localhost:8080/api/documents/123
```

**Success Response (200 OK):**

```json
{
  "id": 123,
  "filename": "vacation-2024.pdf",
  "originalFilename": "vacation-2024.pdf",
  "fileSize": 2048576,
  "uploadTimestamp": "2024-01-15T10:30:00",
  "status": "COMPLETED",
  "chunkCount": 25,
  "errorMessage": null
}
```

**Error Responses:**

- `404 Not Found`: Document does not exist

---

### 6. Get Document Processing Status

Check the current processing status of a document.

**Endpoint:** `GET /documents/{documentId}/status`

**Example Request:**

```bash
curl http://localhost:8080/api/documents/123/status
```

**Success Response (200 OK):**

```json
{
  "documentId": 123,
  "filename": "vacation-2024.pdf",
  "status": "COMPLETED",
  "chunkCount": 25,
  "errorMessage": null,
  "uploadTimestamp": "2024-01-15T10:30:00"
}
```

**Processing Status Values:**

| Status | Description |
|--------|-------------|
| PENDING | Document queued for processing |
| PROCESSING | Text extraction and embedding in progress |
| COMPLETED | Successfully processed and indexed |
| FAILED | Processing failed (see errorMessage) |

---

### 7. Delete Document

Delete a document and all its associated chunks permanently.

**Endpoint:** `DELETE /documents/{documentId}`

**Example Request:**

```bash
curl -X DELETE http://localhost:8080/api/documents/123
```

**Success Response:** `204 No Content`

**Error Responses:**

- `404 Not Found`: Document does not exist

---

### 8. Health Check

Check the health status of the application and its dependencies.

**Endpoint:** `GET /health`

**Example Request:**

```bash
curl http://localhost:8080/api/health
```

**Success Response (200 OK):**

```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:40:00",
  "database": "CONNECTED",
  "nvidiaApi": "AVAILABLE"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| status | String | Overall health status (UP/DOWN) |
| timestamp | String | Current server timestamp |
| database | String | Database connection status |
| nvidiaApi | String | NVIDIA API availability |

---

### 9. Diagnostic Information

Get detailed diagnostic information about the system (useful for debugging).

**Endpoint:** `GET /diagnostic`

**Example Request:**

```bash
curl http://localhost:8080/api/diagnostic
```

**Success Response (200 OK):**

```json
{
  "timestamp": "2024-01-15T10:45:00",
  "systemInfo": {
    "javaVersion": "17.0.8",
    "springBootVersion": "3.5.12",
    "activeProfile": "dev"
  },
  "database": {
    "status": "CONNECTED",
    "totalDocuments": 45,
    "totalChunks": 678,
    "connectionPoolSize": 10
  },
  "nvidia": {
    "status": "AVAILABLE",
    "embeddingModel": "nvidia/nv-embed-v1",
    "chatModel": "meta/llama-3.1-70b-instruct"
  },
  "cache": {
    "embeddingCacheSize": 123,
    "retrievalCacheSize": 45,
    "hitRate": 0.78
  }
}
```

---

## Error Handling

All endpoints follow a consistent error response format:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/upload/text",
  "details": [
    "Content must not be empty",
    "Filename must not be blank"
  ]
}
```

**Common HTTP Status Codes:**

| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Bad Request - Invalid input |
| 404 | Not Found - Resource doesn't exist |
| 413 | Payload Too Large - File size exceeded |
| 415 | Unsupported Media Type - Invalid file format |
| 500 | Internal Server Error - Server-side error |
| 503 | Service Unavailable - External service down |

---

## Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Limit:** 20 requests per second per client
- **Window:** 1 second rolling window
- **Response:** 429 Too Many Requests when limit exceeded

**Rate Limit Headers:**

```
X-RateLimit-Limit: 20
X-RateLimit-Remaining: 15
X-RateLimit-Reset: 1642248000
```

---

## Pagination

Currently, the API does not support pagination. Future versions will implement cursor-based pagination for listing documents.

---

## Versioning

The API is currently at version 1.0.0. Future versions will use URL-based versioning:

```
/api/v1/chat/query
/api/v2/chat/query
```

---

## OpenAPI Specification

The complete OpenAPI 3.0 specification is available at:

```
http://localhost:8080/v3/api-docs
```

Interactive Swagger UI:

```
http://localhost:8080/swagger-ui/index.html
```

---

## Code Examples

### JavaScript (Fetch API)

```javascript
// Upload text
async function uploadText(content, filename) {
  const response = await fetch('http://localhost:8080/api/upload/text', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ content, filename }),
  });
  return await response.json();
}

// Query memories
async function queryMemories(query) {
  const response = await fetch('http://localhost:8080/api/chat/query', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query }),
  });
  return await response.json();
}

// Usage
const result = await uploadText('My vacation memory...', 'vacation.txt');
const answer = await queryMemories('Tell me about my vacation');
console.log(answer.answer);
```

### Python (Requests)

```python
import requests

# Upload PDF
def upload_pdf(file_path):
    with open(file_path, 'rb') as f:
        files = {'file': f}
        response = requests.post(
            'http://localhost:8080/api/upload/pdf',
            files=files
        )
    return response.json()

# Query memories
def query_memories(query):
    response = requests.post(
        'http://localhost:8080/api/chat/query',
        json={'query': query}
    )
    return response.json()

# Usage
result = upload_pdf('document.pdf')
answer = query_memories('What does the document say?')
print(answer['answer'])
```

### Java (RestTemplate)

```java
RestTemplate restTemplate = new RestTemplate();

// Upload text
TextDocumentRequest request = new TextDocumentRequest();
request.setContent("My memory...");
request.setFilename("memory.txt");

DocumentUploadResponse response = restTemplate.postForObject(
    "http://localhost:8080/api/upload/text",
    request,
    DocumentUploadResponse.class
);

// Query memories
ChatRequest chatRequest = new ChatRequest();
chatRequest.setQuery("Tell me about my memories");

ChatResponse chatResponse = restTemplate.postForObject(
    "http://localhost:8080/api/chat/query",
    chatRequest,
    ChatResponse.class
);

System.out.println(chatResponse.getAnswer());
```

---

## WebSocket Support

WebSocket support for real-time chat is planned for future versions.

---

## Webhooks

Webhook support for document processing notifications is planned for future versions.
