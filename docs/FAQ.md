# Frequently Asked Questions (FAQ)

Common questions and answers about the Intelligent Memory Preservation & Retrieval System.

## General Questions

### What is IMPRS?

IMPRS (Intelligent Memory Preservation & Retrieval System) is an AI-powered platform designed to help Alzheimer's and dementia patients preserve and recall their personal memories. It uses advanced RAG (Retrieval-Augmented Generation) architecture with vector embeddings and semantic search to enable natural language queries about stored memories.

### Who is this system for?

- Alzheimer's and dementia patients
- Caregivers and family members
- Healthcare professionals
- Anyone wanting to preserve and organize personal memories

### What file formats are supported?

Currently supported:
- PDF documents
- Plain text (.txt)

Future support planned for:
- Images (JPEG, PNG)
- Audio recordings
- Video files
- Microsoft Word documents

### Is my data secure?

Yes. The system implements:
- Input validation and sanitization
- SQL injection prevention
- Secure credential management
- Rate limiting
- No data sharing with third parties (except NVIDIA API for processing)

---

## Technical Questions

### What AI models are used?

- **Embeddings**: NVIDIA NV-Embed-v1 (4096 dimensions)
- **Chat/LLM**: Meta Llama 3.1 70B Instruct
- **Vector Search**: pgvector with cosine similarity

### How does the RAG pipeline work?

1. Documents are uploaded and split into chunks
2. Each chunk is converted to a vector embedding
3. Embeddings are stored in PostgreSQL with pgvector
4. User queries are converted to embeddings
5. Similar chunks are retrieved via vector search
6. Retrieved chunks provide context for AI response generation

### What is vector similarity search?

Vector similarity search finds semantically similar content by comparing vector embeddings. Unlike keyword search, it understands meaning and context, enabling queries like "happy moments" to find relevant memories even if those exact words aren't present.

### How accurate is the search?

The system uses cosine similarity with a configurable threshold (default: 0.25). Typical accuracy:
- High relevance (>0.7): Very accurate matches
- Medium relevance (0.4-0.7): Related content
- Low relevance (<0.4): Filtered out

### Can I adjust search sensitivity?

Yes, modify in `application.properties`:
```properties
app.retrieval.similarity-threshold=0.25  # Lower = more results
app.retrieval.top-k=8                    # Number of results
```

---

## Setup and Installation

### What are the system requirements?

**Minimum:**
- Java 17
- 4GB RAM
- 10GB disk space
- Docker (for database)

**Recommended:**
- Java 17
- 8GB RAM
- 50GB disk space
- Docker with 4GB memory allocation

### Do I need an NVIDIA GPU?

No. The system uses NVIDIA's cloud API (NIM), not local GPU processing. You only need an API key from [build.nvidia.com](https://build.nvidia.com).

### How do I get an NVIDIA API key?

1. Visit [https://build.nvidia.com](https://build.nvidia.com)
2. Sign up for a free account
3. Navigate to API Keys section
4. Generate a new API key
5. Copy the key to your `.env` file

### Can I use a different LLM provider?

Yes, the system is designed to be provider-agnostic. You can implement alternative providers by:
1. Creating a new client class (similar to `NvidiaChatClient`)
2. Implementing the same interface
3. Configuring via Spring profiles

### Installation fails on Windows - what should I do?

Common Windows issues:

**Docker not starting:**
```bash
# Enable WSL 2
wsl --install
wsl --set-default-version 2

# Restart Docker Desktop
```

**Maven wrapper not executable:**
```bash
# Use mvnw.cmd instead
mvnw.cmd clean install
```

**Port 5432 already in use:**
```bash
# Stop existing PostgreSQL
net stop postgresql-x64-16

# Or use different port in docker-compose.yml
ports:
  - "5433:5432"
```

---

## Usage Questions

### How do I upload documents?

**Via API:**
```bash
curl -X POST http://localhost:8080/api/upload/pdf \
  -F "file=@document.pdf"
```

**Via Swagger UI:**
1. Open http://localhost:8080/swagger-ui/index.html
2. Navigate to Upload Controller
3. Click "Try it out"
4. Select file and execute

### How do I query my memories?

**Via API:**
```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Tell me about my vacation"}'
```

**Via Swagger UI:**
1. Open http://localhost:8080/swagger-ui/index.html
2. Navigate to Chat Controller
3. Enter your query and execute

### Why am I getting "No relevant memories found"?

Possible reasons:
1. **No documents uploaded**: Upload some documents first
2. **Similarity threshold too high**: Lower the threshold in config
3. **Query too specific**: Try broader queries
4. **Documents not processed**: Check processing status

### How long does document processing take?

Processing time depends on document size:
- Small text (< 1 page): < 1 second
- Medium PDF (10-50 pages): 5-15 seconds
- Large PDF (100+ pages): 30-60 seconds

Processing happens asynchronously, so API responds immediately.

### Can I delete uploaded documents?

Currently, deletion is not exposed via API. To delete manually:

```sql
-- Connect to database
docker exec -it rag-postgres psql -U raguser ragdb

-- Delete document and its chunks
DELETE FROM document_chunks WHERE document_id = 123;
DELETE FROM documents WHERE id = 123;
```

---

## Performance Questions

### How many documents can the system handle?

The system scales based on your database:
- **Development**: 1,000+ documents
- **Production (optimized)**: 100,000+ documents
- **Enterprise (clustered)**: Millions of documents

### How fast are queries?

Typical response times:
- Vector search: 50-200ms
- LLM generation: 1-3 seconds
- Total query time: 1.5-4 seconds

With caching:
- Cached embeddings: 10-50ms
- Cached retrievals: 5-20ms

### How can I improve performance?

1. **Enable caching** (already enabled by default)
2. **Optimize vector index**:
   ```sql
   REINDEX INDEX idx_chunks_embedding;
   ANALYZE document_chunks;
   ```
3. **Increase connection pool**:
   ```properties
   spring.datasource.hikari.maximum-pool-size=50
   ```
4. **Use production profile**:
   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=prod
   ```

### Why is the first query slow?

The first query after startup is slower due to:
- JVM warmup
- Database connection initialization
- Cache population
- Model loading

Subsequent queries are much faster (80-90% improvement).

---

## Troubleshooting

### "Database connection failed" error

**Check database status:**
```bash
docker ps | grep postgres
```

**Restart database:**
```bash
docker-compose restart postgres
```

**Verify credentials:**
```bash
# Check .env file
cat .env | grep DB_
```

### "NVIDIA API error" or "Unauthorized"

**Verify API key:**
```bash
echo $NVIDIA_API_KEY
```

**Test API key:**
```bash
curl -H "Authorization: Bearer $NVIDIA_API_KEY" \
     https://integrate.api.nvidia.com/v1/models
```

**Check rate limits:**
- Free tier: 1,000 requests/day
- If exceeded, wait 24 hours or upgrade plan

### "Out of memory" error

**Increase JVM heap:**
```bash
export MAVEN_OPTS="-Xmx4g"
./mvnw spring-boot:run
```

**Reduce batch size:**
```properties
app.embedding.batch-size=50  # Default: 100
```

### Application won't start

**Check port availability:**
```bash
lsof -i :8080
```

**Check logs:**
```bash
tail -f logs/spring.log
```

**Verify Java version:**
```bash
java -version
# Must be 17 or higher
```

### Queries return irrelevant results

**Lower similarity threshold:**
```properties
app.retrieval.similarity-threshold=0.15  # Default: 0.25
```

**Increase result count:**
```properties
app.retrieval.top-k=15  # Default: 8
```

**Check document quality:**
- Ensure documents contain relevant content
- Verify text extraction worked correctly
- Check for encoding issues

---

## API Questions

### Is there a rate limit?

Yes:
- **Default**: 20 requests/second per client
- **Burst**: Up to 40 requests (2-second burst)
- **NVIDIA API**: 1,000 requests/day (free tier)

### How do I authenticate?

Currently, no authentication is required. Future versions will implement:
- JWT-based authentication
- API key authentication
- OAuth 2.0 support

### Can I use the API from JavaScript?

Yes! Example:
```javascript
const response = await fetch('http://localhost:8080/api/chat/query', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ query: 'Tell me about my memories' })
});
const data = await response.json();
console.log(data.answer);
```

### Is there a Python client?

Not officially, but you can use `requests`:
```python
import requests

response = requests.post(
    'http://localhost:8080/api/chat/query',
    json={'query': 'Tell me about my memories'}
)
print(response.json()['answer'])
```

### Where is the API documentation?

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Markdown**: [docs/API_REFERENCE.md](API_REFERENCE.md)

---

## Deployment Questions

### Can I deploy to production?

Yes! See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions.

### What cloud providers are supported?

- AWS (Elastic Beanstalk, ECS, EC2)
- Google Cloud Platform (Cloud Run, GKE)
- Azure (App Service, AKS)
- Any platform supporting Docker

### Do I need a separate database server?

For production, yes. Use:
- AWS RDS PostgreSQL
- Google Cloud SQL
- Azure Database for PostgreSQL
- Self-hosted PostgreSQL cluster

### How do I enable HTTPS?

Use a reverse proxy (Nginx, Apache) or cloud load balancer:

```nginx
server {
    listen 443 ssl;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://localhost:8080;
    }
}
```

### How do I scale horizontally?

1. Make application stateless (already is)
2. Use external cache (Redis)
3. Use database read replicas
4. Deploy multiple instances behind load balancer

---

## Development Questions

### How do I contribute?

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

See [DEVELOPMENT.md](DEVELOPMENT.md) for details.

### What's the tech stack?

- **Backend**: Spring Boot 3.5.12, Java 17
- **Database**: PostgreSQL 16 + pgvector
- **AI**: NVIDIA NIM API, Spring AI
- **Caching**: Caffeine
- **Resilience**: Resilience4j
- **Build**: Maven
- **Deployment**: Docker

### How do I run tests?

```bash
# All tests
./mvnw test

# Specific test
./mvnw test -Dtest=ChatServiceImplTest

# With coverage
./mvnw test jacoco:report
```

### How do I add a new feature?

1. Create feature branch
2. Implement feature with tests
3. Update documentation
4. Submit pull request

See [DEVELOPMENT.md](DEVELOPMENT.md) for coding standards.

---

## Future Features

### What features are planned?

- Multi-user support with authentication
- Image and video memory support
- Voice interface
- Mobile applications (iOS/Android)
- Family sharing features
- Advanced analytics dashboard
- Export memories to PDF
- Timeline visualization

### When will feature X be available?

Check the [GitHub Issues](https://github.com/your-repo/issues) and [Roadmap](../README.md#roadmap) for planned features and timelines.

### Can I request a feature?

Yes! Create an issue on GitHub with:
- Feature description
- Use case
- Expected behavior
- Any relevant examples

---

## Support

### Where can I get help?

- **Documentation**: Check [docs/](.)
- **GitHub Issues**: Report bugs or ask questions
- **Email**: support@example.com (if available)
- **Community**: Join our Discord/Slack (if available)

### How do I report a bug?

Create a GitHub issue with:
1. Description of the problem
2. Steps to reproduce
3. Expected vs actual behavior
4. System information (OS, Java version)
5. Relevant logs or error messages

### Is commercial support available?

Contact us for enterprise support options including:
- Priority bug fixes
- Custom feature development
- Training and onboarding
- SLA guarantees
- Dedicated support channel
