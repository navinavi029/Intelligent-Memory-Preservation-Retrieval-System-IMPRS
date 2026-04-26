# Development Guide

Complete guide for developers working on the Intelligent Memory Preservation & Retrieval System.

## Table of Contents

1. [Development Setup](#development-setup)
2. [Project Structure](#project-structure)
3. [Coding Standards](#coding-standards)
4. [Testing Guidelines](#testing-guidelines)
5. [Git Workflow](#git-workflow)
6. [Common Tasks](#common-tasks)
7. [Troubleshooting](#troubleshooting)
8. [Performance Tips](#performance-tips)

---

## Development Setup

### IDE Setup

#### IntelliJ IDEA (Recommended)

1. **Install Plugins:**
   - Lombok
   - Spring Boot
   - Docker
   - Database Navigator

2. **Import Project:**
   ```
   File → Open → Select demo/pom.xml
   ```

3. **Enable Annotation Processing:**
   ```
   Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   ✓ Enable annotation processing
   ```

4. **Configure Code Style:**
   ```
   Settings → Editor → Code Style → Java
   - Indent: 4 spaces
   - Continuation indent: 8 spaces
   - Use tab character: No
   ```

#### VS Code

1. **Install Extensions:**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support
   - Docker

2. **Configure Settings:**
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.compile.nullAnalysis.mode": "automatic",
     "spring-boot.ls.java.home": "/path/to/java17"
   }
   ```

### Local Environment

1. **Install Dependencies:**
   ```bash
   # Java 17
   sdk install java 17.0.8-tem
   sdk use java 17.0.8-tem
   
   # Maven
   sdk install maven 3.9.5
   
   # Docker Desktop
   # Download from https://www.docker.com/products/docker-desktop
   ```

2. **Setup Environment Variables:**
   ```bash
   # Add to ~/.bashrc or ~/.zshrc
   export NVIDIA_API_KEY="nvapi-your-key-here"
   export JAVA_HOME=$HOME/.sdkman/candidates/java/current
   export PATH=$JAVA_HOME/bin:$PATH
   ```

3. **Start Development Database:**
   ```bash
   docker-compose up -d
   ```

4. **Verify Setup:**
   ```bash
   java -version
   mvn -version
   docker ps
   ```

---

## Project Structure

```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── AppConfig.java
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   ├── NvidiaConfig.java
│   │   │   │   ├── ResilienceConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   │
│   │   │   ├── controller/          # REST endpoints
│   │   │   │   ├── ChatController.java
│   │   │   │   ├── UploadController.java
│   │   │   │   ├── HealthController.java
│   │   │   │   └── DiagnosticController.java
│   │   │   │
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── ChatRequest.java
│   │   │   │   ├── ChatResponse.java
│   │   │   │   ├── DocumentUploadResponse.java
│   │   │   │   ├── RetrievedChunk.java
│   │   │   │   └── SourceReference.java
│   │   │   │
│   │   │   ├── exception/           # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── FileUploadException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   │
│   │   │   ├── model/               # JPA entities
│   │   │   │   ├── Document.java
│   │   │   │   ├── DocumentChunk.java
│   │   │   │   └── ProcessingStatus.java
│   │   │   │
│   │   │   ├── repository/          # Data access
│   │   │   │   ├── DocumentRepository.java
│   │   │   │   └── ChunkRepository.java
│   │   │   │
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── ChatServiceImpl.java
│   │   │   │   ├── EmbeddingService.java
│   │   │   │   ├── EmbeddingServiceImpl.java
│   │   │   │   ├── RetrievalService.java
│   │   │   │   ├── RetrievalServiceImpl.java
│   │   │   │   ├── PdfProcessingService.java
│   │   │   │   ├── TextProcessingService.java
│   │   │   │   └── ResilientNvidiaChatClient.java
│   │   │   │
│   │   │   └── DemoApplication.java # Main application
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── schema.sql
│   │
│   └── test/
│       └── java/com/example/demo/
│           ├── controller/
│           ├── service/
│           └── repository/
│
├── pom.xml                          # Maven configuration
├── Dockerfile                       # Docker image definition
└── README.md
```

### Package Organization

- **config**: Spring configuration beans
- **controller**: REST API endpoints (thin layer)
- **dto**: Request/response objects
- **exception**: Custom exceptions and handlers
- **model**: Database entities
- **repository**: Data access interfaces
- **service**: Business logic (thick layer)

---

## Coding Standards

### Java Style Guide

#### Naming Conventions

```java
// Classes: PascalCase
public class ChatService { }

// Interfaces: PascalCase (no 'I' prefix)
public interface EmbeddingService { }

// Methods: camelCase
public void processQuery() { }

// Variables: camelCase
private String queryText;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRIES = 3;

// Packages: lowercase
package com.example.demo.service;
```

#### Code Organization

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    
    // 1. Constants
    private static final String SYSTEM_PROMPT = "...";
    
    // 2. Dependencies (injected via constructor)
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    
    // 3. Public methods
    @Override
    public ChatResponse processQuery(String query) {
        // Implementation
    }
    
    // 4. Private helper methods
    private String sanitizeInput(String input) {
        // Implementation
    }
}
```

#### Documentation

```java
/**
 * Process a user query and generate an AI response.
 * 
 * This method:
 * 1. Sanitizes the input
 * 2. Generates query embedding
 * 3. Retrieves relevant chunks
 * 4. Constructs context window
 * 5. Generates AI response
 * 
 * @param query User's natural language query
 * @return ChatResponse with answer and source citations
 * @throws IllegalArgumentException if query is null or empty
 */
@Override
public ChatResponse processQuery(String query) {
    // Implementation
}
```

#### Logging

```java
// Use SLF4J with Lombok @Slf4j
@Slf4j
public class ChatServiceImpl {
    
    public void processQuery(String query) {
        // INFO: Important business events
        log.info("Processing query - length: {}", query.length());
        
        // DEBUG: Detailed diagnostic information
        log.debug("Query embedding generated - dimensions: {}", embedding.length);
        
        // WARN: Recoverable errors or degraded performance
        log.warn("Retrieval returned no results - threshold may be too high");
        
        // ERROR: System errors requiring attention
        log.error("Failed to generate embedding - error: {}", e.getMessage(), e);
        
        // TRACE: Very detailed debugging (rarely used)
        log.trace("Embedding values: {}", Arrays.toString(embedding));
    }
}
```

### Spring Boot Best Practices

#### Dependency Injection

```java
// ✅ Good: Constructor injection with Lombok
@Service
@RequiredArgsConstructor
public class ChatServiceImpl {
    private final EmbeddingService embeddingService;
}

// ❌ Bad: Field injection
@Service
public class ChatServiceImpl {
    @Autowired
    private EmbeddingService embeddingService;
}
```

#### Configuration Properties

```java
// ✅ Good: Type-safe configuration
@ConfigurationProperties(prefix = "app.retrieval")
@Data
public class RetrievalConfig {
    private int topK = 8;
    private double similarityThreshold = 0.25;
}

// ❌ Bad: Direct @Value injection everywhere
@Value("${app.retrieval.top-k}")
private int topK;
```

#### Exception Handling

```java
// ✅ Good: Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUpload(FileUploadException ex) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .message(ex.getMessage())
                .build()
        );
    }
}

// ❌ Bad: Try-catch in every controller method
@PostMapping("/upload")
public ResponseEntity<?> upload() {
    try {
        // ...
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

---

## Testing Guidelines

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {
    
    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private RetrievalService retrievalService;
    
    @InjectMocks
    private ChatServiceImpl chatService;
    
    @Test
    void processQuery_withValidQuery_returnsResponse() {
        // Arrange
        String query = "Tell me about my vacation";
        float[] embedding = new float[]{0.1f, 0.2f};
        List<RetrievedChunk> chunks = List.of(
            RetrievedChunk.builder()
                .content("Vacation memory...")
                .similarityScore(0.85)
                .build()
        );
        
        when(embeddingService.generateQueryEmbedding(query))
            .thenReturn(embedding);
        when(retrievalService.retrieveSimilarChunks(embedding, 8, 0.25))
            .thenReturn(chunks);
        
        // Act
        ChatResponse response = chatService.processQuery(query);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertEquals(1, response.getSources().size());
        
        verify(embeddingService).generateQueryEmbedding(query);
        verify(retrievalService).retrieveSimilarChunks(embedding, 8, 0.25);
    }
    
    @Test
    void processQuery_withEmptyQuery_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> chatService.processQuery(""));
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void queryEndpoint_withValidRequest_returns200() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuery("Tell me about my memories");
        
        mockMvc.perform(post("/api/chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").exists())
            .andExpect(jsonPath("$.sources").isArray());
    }
}
```

### Repository Tests

```java
@DataJpaTest
class ChunkRepositoryTest {
    
    @Autowired
    private ChunkRepository chunkRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void findSimilarChunks_returnsMatchingChunks() {
        // Arrange
        DocumentChunk chunk = new DocumentChunk();
        chunk.setContent("Test content");
        chunk.setEmbedding(new float[]{0.1f, 0.2f});
        entityManager.persist(chunk);
        entityManager.flush();
        
        // Act
        List<Object[]> results = chunkRepository.findSimilarChunks(
            "[0.1,0.2]", 0.5, 10
        );
        
        // Assert
        assertFalse(results.isEmpty());
    }
}
```

### Test Coverage

Run tests with coverage:
```bash
./mvnw clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

Target coverage:
- Overall: 80%+
- Service layer: 90%+
- Controller layer: 85%+
- Repository layer: 70%+

---

## Git Workflow

### Branch Strategy

```
main (production)
  ├── develop (integration)
  │   ├── feature/add-image-support
  │   ├── feature/improve-chunking
  │   └── bugfix/fix-embedding-cache
  └── hotfix/critical-security-fix
```

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Format
<type>(<scope>): <subject>

<body>

<footer>

# Examples
feat(chat): add support for multi-turn conversations

Implements conversation history tracking and context management
for multi-turn chat interactions.

Closes #123

fix(embedding): resolve cache invalidation issue

The embedding cache was not properly invalidating when documents
were deleted, causing stale results.

Fixes #456

docs(api): update API documentation for new endpoints

refactor(service): extract common validation logic

test(chat): add integration tests for error scenarios

chore(deps): upgrade Spring Boot to 3.5.12
```

### Pull Request Process

1. **Create Feature Branch:**
   ```bash
   git checkout -b feature/my-feature develop
   ```

2. **Make Changes and Commit:**
   ```bash
   git add .
   git commit -m "feat(scope): description"
   ```

3. **Push and Create PR:**
   ```bash
   git push origin feature/my-feature
   # Create PR on GitHub
   ```

4. **PR Checklist:**
   - [ ] Tests pass locally
   - [ ] Code follows style guide
   - [ ] Documentation updated
   - [ ] No merge conflicts
   - [ ] Reviewed by at least one person

5. **Merge:**
   ```bash
   # Squash and merge to develop
   git checkout develop
   git merge --squash feature/my-feature
   git commit -m "feat(scope): description"
   ```

---

## Common Tasks

### Adding a New Endpoint

1. **Create DTO:**
   ```java
   @Data
   @Builder
   public class MyRequest {
       @NotBlank
       private String field;
   }
   ```

2. **Create Controller:**
   ```java
   @RestController
   @RequestMapping("/api/my-resource")
   public class MyController {
       
       @PostMapping
       public ResponseEntity<MyResponse> create(@Valid @RequestBody MyRequest request) {
           // Implementation
       }
   }
   ```

3. **Add Service Logic:**
   ```java
   @Service
   public class MyService {
       public MyResponse process(MyRequest request) {
           // Business logic
       }
   }
   ```

4. **Add Tests:**
   ```java
   @Test
   void create_withValidRequest_returns200() {
       // Test implementation
   }
   ```

### Adding a New Configuration Property

1. **Add to application.properties:**
   ```properties
   app.my-feature.enabled=true
   app.my-feature.timeout=30
   ```

2. **Create Configuration Class:**
   ```java
   @ConfigurationProperties(prefix = "app.my-feature")
   @Data
   public class MyFeatureConfig {
       private boolean enabled = true;
       private int timeout = 30;
   }
   ```

3. **Enable in Main Class:**
   ```java
   @EnableConfigurationProperties(MyFeatureConfig.class)
   public class DemoApplication {
       // ...
   }
   ```

### Database Migration

1. **Create Migration Script:**
   ```sql
   -- V2__add_new_table.sql
   CREATE TABLE new_table (
       id BIGSERIAL PRIMARY KEY,
       name VARCHAR(255) NOT NULL
   );
   ```

2. **Update Entity:**
   ```java
   @Entity
   @Table(name = "new_table")
   public class NewEntity {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       
       private String name;
   }
   ```

3. **Test Migration:**
   ```bash
   ./mvnw flyway:migrate
   ```

---

## Troubleshooting

### Common Issues

#### Database Connection Failed

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs rag-postgres

# Restart database
docker-compose restart postgres
```

#### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

#### Out of Memory

```bash
# Increase heap size
export MAVEN_OPTS="-Xmx2g"
./mvnw spring-boot:run
```

#### Tests Failing

```bash
# Run specific test
./mvnw test -Dtest=ChatServiceImplTest

# Skip tests
./mvnw clean install -DskipTests

# Debug test
./mvnw test -Dmaven.surefire.debug
```

---

## Performance Tips

### Optimize Database Queries

```java
// ✅ Good: Fetch only needed fields
@Query("SELECT new com.example.dto.ChunkDto(c.id, c.content) FROM DocumentChunk c")
List<ChunkDto> findAllChunks();

// ❌ Bad: Fetch entire entity
List<DocumentChunk> findAll();
```

### Use Caching Wisely

```java
// Cache expensive operations
@Cacheable(value = "embeddings", key = "#text")
public float[] generateEmbedding(String text) {
    // Expensive API call
}

// Evict cache when data changes
@CacheEvict(value = "embeddings", allEntries = true)
public void clearCache() {
    // Cache cleared
}
```

### Async Processing

```java
// Process documents asynchronously
@Async
public CompletableFuture<ProcessingResult> processDocument(MultipartFile file) {
    // Long-running operation
    return CompletableFuture.completedFuture(result);
}
```

### Batch Operations

```java
// Process in batches
List<List<String>> batches = Lists.partition(items, 100);
for (List<String> batch : batches) {
    processBatch(batch);
}
```

---

## Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [pgvector Documentation](https://github.com/pgvector/pgvector)
- [NVIDIA NIM API](https://build.nvidia.com/explore/discover)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
