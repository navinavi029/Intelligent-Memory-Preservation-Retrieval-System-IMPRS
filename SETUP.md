# Setup Guide

## Prerequisites

- Docker Desktop (running)
- Java 17+
- Maven (included via mvnw)
- NVIDIA API Key (get from https://build.nvidia.com)

## Quick Setup

### 1. Check Prerequisites
```bash
1_CHECK_PREREQUISITES.bat
```

### 2. Configure API Key
```bash
2_CONFIGURE_API_KEY.bat
```
Enter your NVIDIA API key when prompted.

### 3. Start Application
```bash
3_START_APPLICATION.bat
```
This will:
- Start or reuse existing PostgreSQL container
- Initialize database schema if needed
- Start the Spring Boot application

### 4. Access Swagger UI
```bash
4_OPEN_SWAGGER_UI.bat
```
Or visit: http://localhost:8080/swagger-ui/index.html

### 5. Optimize Database (Recommended)
```bash
5_OPTIMIZE_DATABASE.bat
```
This creates indexes for faster retrieval.

## Stopping

```bash
STOP_DOCKER_DB.bat
```

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:
```
NVIDIA_API_KEY=your_api_key_here
```

### Application Properties

Edit `demo/src/main/resources/application.properties` to customize:

```properties
# NVIDIA Models
nvidia.api.embedding-model=nvidia/nv-embed-v1
nvidia.api.chat-model=nvidia/nemotron-3-super-120b-a12b

# Retrieval Settings
app.retrieval.top-k=8
app.retrieval.similarity-threshold=0.25

# Chunking Strategy
app.chunking.chunk-size=800
app.chunking.overlap=100
```

## Troubleshooting

### Port Already in Use
```
Error: Port 5432 is already allocated
Solution: Stop other PostgreSQL instances or change port in docker-compose.yml
```

### Container Already Exists
The startup script automatically handles this - it will reuse the existing container.

### Database Schema Missing
The startup script checks and initializes the schema automatically.

### Embedding API Errors
- Verify your NVIDIA API key is valid
- Check API rate limits at https://build.nvidia.com
- Review application logs for detailed error messages
