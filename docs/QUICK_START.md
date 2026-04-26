# Quick Start Guide

Get up and running with IMPRS in 5 minutes!

## Prerequisites

- Java 17+ installed
- Docker Desktop running
- NVIDIA API key ([Get one free](https://build.nvidia.com))

## Installation (5 Steps)

### 1. Clone Repository

```bash
git clone <repository-url>
cd IMPRS
```

### 2. Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env and add your NVIDIA API key
# Windows: notepad .env
# Mac/Linux: nano .env
```

Add your API key:
```env
NVIDIA_API_KEY=nvapi-YOUR_ACTUAL_KEY_HERE
```

### 3. Start Database

**Windows:**
```bash
SETUP.bat
```

**Mac/Linux:**
```bash
docker-compose up -d
```

Wait 10 seconds for database to initialize.

### 4. Build Application

```bash
cd demo
./mvnw clean install
```

**Windows:** Use `mvnw.cmd` instead of `./mvnw`

### 5. Run Application

```bash
./mvnw spring-boot:run
```

Wait for: `Started DemoApplication in X seconds`

## Verify Installation

Open your browser:
```
http://localhost:8080/swagger-ui/index.html
```

You should see the API documentation!

## First Steps

### Upload Your First Memory

**Option 1: Using Swagger UI**

1. Open http://localhost:8080/swagger-ui/index.html
2. Find "Upload Controller"
3. Click "POST /api/upload/text"
4. Click "Try it out"
5. Enter:
   ```json
   {
     "memory": "Today I went to the beach with my family. We collected seashells and watched the sunset. It was a beautiful day."
   }
   ```
6. Click "Execute"
7. You should see a success response!

**Option 2: Using cURL**

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "memory": "Today I went to the beach with my family. We collected seashells and watched the sunset. It was a beautiful day."
  }'
```

### Query Your Memory

**Using Swagger UI:**

1. Find "Chat Controller"
2. Click "POST /api/chat/query"
3. Click "Try it out"
4. Enter:
   ```json
   {
     "query": "Tell me about the beach"
   }
   ```
5. Click "Execute"
6. Read the AI-generated response!

**Using cURL:**

```bash
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Tell me about the beach"}'
```

Expected response:
```json
{
  "answer": "Based on your shared memories, you had a wonderful day at the beach with your family. You collected seashells and enjoyed watching the sunset together.",
  "sources": [
    {
      "documentId": 1,
      "filename": "beach-memory.txt",
      "chunkNumber": 1,
      "similarityScore": 0.89
    }
  ],
  "retrievedChunks": 1
}
```

## Common Commands

### Start/Stop Database

**Start:**
```bash
docker-compose up -d
```

**Stop:**
```bash
docker-compose down
```

**Windows:**
```bash
SETUP.bat          # Start
STOP_DOCKER_DB.bat # Stop
```

### Run Application

**Development mode:**
```bash
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

**Production mode:**
```bash
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

### Check Health

```bash
curl http://localhost:8080/api/health
```

### View Logs

**Application logs:**
```bash
# In the terminal where you ran mvnw spring-boot:run
```

**Database logs:**
```bash
docker logs rag-postgres
```

## Troubleshooting

### "Database connection failed"

```bash
# Check if database is running
docker ps | grep postgres

# If not running, start it
docker-compose up -d

# Wait 10 seconds and try again
```

### "Port 8080 already in use"

```bash
# Find what's using the port
# Windows:
netstat -ano | findstr :8080

# Mac/Linux:
lsof -i :8080

# Kill the process or use a different port
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### "NVIDIA API error"

```bash
# Verify your API key is set
# Windows:
echo %NVIDIA_API_KEY%

# Mac/Linux:
echo $NVIDIA_API_KEY

# If empty, edit .env file and restart application
```

### "Out of memory"

```bash
# Increase heap size
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g"
```

## Next Steps

### Upload More Documents

**Upload more text:**
```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "memory": "Your memory text here..."
  }'
```

**List all documents:**
```bash
curl http://localhost:8080/api/documents
```

**Delete a document:**
```bash
curl -X DELETE http://localhost:8080/api/documents/123
```

### Try Different Queries

```bash
# Ask about specific topics
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What happy moments do I have?"}'

# Ask about people
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Tell me about time with my family"}'

# Ask about places
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Where have I traveled?"}'
```

### Explore the API

Open Swagger UI and try all the endpoints:
```
http://localhost:8080/swagger-ui/index.html
```

### Read the Documentation

- **API Reference**: [docs/API_REFERENCE.md](API_REFERENCE.md)
- **Architecture**: [docs/ARCHITECTURE.md](ARCHITECTURE.md)
- **Development Guide**: [docs/DEVELOPMENT.md](DEVELOPMENT.md)
- **FAQ**: [docs/FAQ.md](FAQ.md)

## Configuration Tips

### Adjust Search Sensitivity

Edit `demo/src/main/resources/application.properties`:

```properties
# More results (lower threshold)
app.retrieval.similarity-threshold=0.15

# More chunks per query
app.retrieval.top-k=15
```

Restart the application to apply changes.

### Enable Debug Logging

Edit `application.properties`:

```properties
logging.level.com.example.demo=DEBUG
```

### Change Server Port

```properties
server.port=8081
```

Or via command line:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## Development Workflow

### Make Code Changes

1. Edit files in `demo/src/main/java/`
2. Save changes
3. Restart application (Ctrl+C, then `./mvnw spring-boot:run`)

### Run Tests

```bash
./mvnw test
```

### Build for Production

```bash
./mvnw clean package -DskipTests
java -jar target/intelligent-memory-preservation-system-1.0.0.jar
```

## Getting Help

- **Documentation**: Check [docs/](.)
- **FAQ**: [docs/FAQ.md](FAQ.md)
- **Issues**: Create a GitHub issue
- **Community**: Join our Discord/Slack

## What's Next?

Now that you're up and running:

1. **Upload your documents** - PDFs, text files, memories
2. **Ask questions** - Natural language queries about your content
3. **Explore the API** - Try different endpoints
4. **Customize settings** - Adjust search parameters
5. **Read the docs** - Learn about advanced features

---

**Congratulations! You're now running IMPRS! 🎉**

For more detailed information, see the [full documentation](../README.md).
