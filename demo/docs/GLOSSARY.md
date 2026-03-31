# Glossary

Comprehensive reference of technical terms, acronyms, and concepts used in the PDF RAG Chatbot project.

---

## A

### Actuator
Spring Boot module that provides production-ready features like health checks, metrics, and monitoring endpoints. Accessible at `/actuator/*`.

### ADR (Architecture Decision Record)
Document that captures an important architectural decision along with its context and consequences.

### AI (Artificial Intelligence)
Computer systems that can perform tasks typically requiring human intelligence, such as understanding language and making decisions.

### API (Application Programming Interface)
Set of rules and protocols that allows different software applications to communicate with each other.

### API Key
Secret token used to authenticate requests to external services (e.g., NVIDIA API).

### Async (Asynchronous)
Operations that run in the background without blocking the main thread. Used for PDF processing to prevent HTTP timeouts.

### Auto-configuration
Spring Boot feature that automatically configures beans based on classpath dependencies and properties.

---

## B

### Backoff (Exponential)
Retry strategy where wait time increases exponentially between attempts (1s, 2s, 4s, 8s...). Prevents overwhelming recovering services.

### Batch Processing
Processing multiple items together in a single operation. Used for generating embeddings for multiple chunks at once.

### Bean
Object managed by Spring's IoC (Inversion of Control) container. Configured via `@Bean`, `@Component`, `@Service`, etc.

### Bucket4j
Java library for rate limiting using token bucket algorithm.

---

## C

### Cache
Temporary storage that saves frequently accessed data for faster retrieval. Reduces API calls and improves response time.

### Cache Hit
When requested data is found in cache, avoiding expensive recomputation.

### Cache Miss
When requested data is not in cache, requiring fresh computation.

### Caffeine
High-performance Java caching library used for in-memory caching.

### Chunk
Small piece of text extracted from a document. Default size: 500 tokens with 50-token overlap.

### Circuit Breaker
Design pattern that prevents cascading failures by stopping requests to failing services. States: CLOSED (normal), OPEN (failing), HALF_OPEN (testing recovery).

### CLOSED (Circuit Breaker State)
Normal state where requests pass through. Transitions to OPEN after failure threshold is reached.

### Concurrent
Multiple operations happening at the same time, often using multiple threads.

### Connection Pool
Reusable set of database connections that improves performance by avoiding connection overhead.

### CORS (Cross-Origin Resource Sharing)
Security mechanism that controls which domains can access your API from browsers.

### Cosine Distance
Measure of dissimilarity between vectors. Formula: `1 - cosine_similarity`. Used in pgvector queries.

### Cosine Similarity
Measure of similarity between two vectors, ranging from -1 (opposite) to 1 (identical). Formula: `(A · B) / (||A|| × ||B||)`.

### CRUD
Create, Read, Update, Delete - basic database operations.

### CSP (Content Security Policy)
HTTP header that prevents XSS attacks by controlling which resources can be loaded.

### CSRF (Cross-Site Request Forgery)
Attack where unauthorized commands are transmitted from a user the web application trusts.

---

## D

### DAO (Data Access Object)
Pattern for abstracting database access. In Spring, repositories serve this role.

### Dependency Injection
Design pattern where objects receive their dependencies from external sources rather than creating them. Core Spring feature.

### DTO (Data Transfer Object)
Object that carries data between processes. Used for API requests/responses.

### Durable Execution
Execution that can survive failures and resume from where it left off.

---

## E

### Embedding
Vector (array of numbers) representing the semantic meaning of text. This project uses 2048-dimensional embeddings.

### Endpoint
Specific URL path that performs an operation (e.g., `/api/chat/query`).

### Entity
Java class mapped to a database table using JPA annotations.

### Error Budget
Acceptable amount of downtime or errors. Used in SLO (Service Level Objective) calculations.

### Event-Driven
Architecture where components communicate through events rather than direct calls.

### Exponential Backoff
See **Backoff (Exponential)**.

---

## F

### Failure Rate
Percentage of failed requests. Circuit breaker opens when this exceeds threshold (default: 50%).

### Feature Flag
Toggle that enables/disables features without code deployment.

### Foreign Key
Database column that references the primary key of another table. Ensures referential integrity.

---

## G

### Graceful Degradation
System continues operating with reduced functionality when components fail.

### GraphQL
Query language for APIs that allows clients to request exactly the data they need.

---

## H

### HALF_OPEN (Circuit Breaker State)
Testing state where circuit breaker allows one request to check if service has recovered.

### Health Check
Endpoint that reports system health status. Used by load balancers and monitoring tools.

### Hibernate
ORM (Object-Relational Mapping) framework used by Spring Data JPA.

### HNSW (Hierarchical Navigable Small World)
Graph-based vector index algorithm. Fast but limited to 2000 dimensions (not used in this project).

### HTTP Status Codes
- **200 OK**: Success
- **202 Accepted**: Request accepted for async processing
- **400 Bad Request**: Invalid request
- **404 Not Found**: Resource doesn't exist
- **413 Payload Too Large**: File exceeds size limit
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server error
- **503 Service Unavailable**: Circuit breaker open or service down

---

## I

### Idempotent
Operation that produces the same result regardless of how many times it's executed.

### Index (Database)
Data structure that improves query performance. This project uses B-tree and IVFFlat indexes.

### Injection Attack
Security vulnerability where malicious code is inserted into queries or commands.

### IoC (Inversion of Control)
Design principle where framework controls object creation and lifecycle. Core to Spring.

### IVFFlat (Inverted File with Flat Compression)
Vector index algorithm that divides vectors into clusters. Supports up to 2048 dimensions. Used in this project.

---

## J

### JAR (Java Archive)
Packaged Java application containing compiled classes and resources.

### Jitter
Random variation added to retry delays to prevent thundering herd problem.

### JPA (Java Persistence API)
Java specification for ORM. Implemented by Hibernate.

### JSON (JavaScript Object Notation)
Lightweight data interchange format. Used for API requests/responses.

### JWT (JSON Web Token)
Compact token format for securely transmitting information between parties. Used for authentication.

---

## K

### K8s
See **Kubernetes**.

### Kubernetes
Container orchestration platform for automating deployment, scaling, and management.

---

## L

### Latency
Time delay between request and response. Target: < 3 seconds for queries.

### LLM (Large Language Model)
AI model trained on vast amounts of text data. This project uses NVIDIA's Llama 3.1-8B-Instruct.

### Load Balancer
Distributes incoming traffic across multiple servers.

### Liveness Probe
Kubernetes health check that determines if container should be restarted.

### LRU (Least Recently Used)
Cache eviction policy that removes least recently accessed items first.

---

## M

### Maven
Build automation and dependency management tool for Java projects.

### Metrics
Quantitative measurements of system behavior (response time, error rate, etc.).

### Microservices
Architectural style where application is composed of small, independent services.

### Micrometer
Metrics collection library that integrates with monitoring systems like Prometheus.

### Migration
Process of updating database schema or moving between versions.

### Monolith
Application where all components are tightly coupled in a single codebase.

### Multi-tenancy
Architecture where single application instance serves multiple customers (tenants).

---

## N

### NemoRetriever
NVIDIA's embedding model that generates 2048-dimensional vectors. Model: `llama-3.2-nemoretriever-300m-embed-v1`.

### NIM (NVIDIA Inference Microservices)
NVIDIA's platform for deploying AI models as microservices.

### NoSQL
Non-relational database. This project uses PostgreSQL (SQL) with vector extension.

---

## O

### OAuth2
Authorization framework for delegated access. Used for third-party authentication.

### Observability
Ability to understand system internal state from external outputs (logs, metrics, traces).

### OPEN (Circuit Breaker State)
Failed state where circuit breaker rejects all requests immediately without calling service.

### ORM (Object-Relational Mapping)
Technique for converting between object-oriented and relational database representations.

### Overlap
Number of tokens shared between consecutive chunks. Default: 50 tokens. Prevents context loss at chunk boundaries.

---

## P

### Pagination
Dividing large result sets into smaller pages for better performance.

### PDFBox
Apache library for extracting text from PDF files.

### pgvector
PostgreSQL extension for storing and querying vector embeddings.

### PII (Personally Identifiable Information)
Data that can identify a specific individual (name, email, SSN, etc.).

### Pooling
See **Connection Pool** or **Thread Pool**.

### PostgreSQL
Open-source relational database. This project uses version 14+ with pgvector extension.

### Prometheus
Open-source monitoring system that collects metrics via HTTP.

### Prompt
Input text given to an LLM to generate a response.

---

## Q

### Query
Request for data from database or search system.

### Query Embedding
Vector representation of user's question. Used to find similar document chunks.

### Queue
Data structure for managing tasks in order (FIFO - First In, First Out).

---

## R

### RAG (Retrieval-Augmented Generation)
AI technique that combines information retrieval with text generation. Finds relevant context before generating response.

### Rate Limiting
Restricting number of requests a client can make in a time period. Default: 100 requests/minute.

### Readiness Probe
Kubernetes health check that determines if container can receive traffic.

### Redis
In-memory data store often used for caching and session management.

### Repository
Spring Data interface for database operations. Extends `JpaRepository`.

### Resilience
System's ability to handle and recover from failures.

### Resilience4j
Java library providing resilience patterns (circuit breaker, retry, rate limiter).

### REST (Representational State Transfer)
Architectural style for web APIs using HTTP methods (GET, POST, PUT, DELETE).

### Retry
Pattern that automatically retries failed operations. This project uses 3 attempts with exponential backoff.

### Rollback
Reverting to previous state after failed operation or deployment.

---

## S

### Saga
Pattern for managing distributed transactions across multiple services.

### Scaling (Horizontal)
Adding more servers to handle increased load.

### Scaling (Vertical)
Adding more resources (CPU, RAM) to existing server.

### Schema
Structure of database tables, columns, and relationships.

### Semantic Search
Search based on meaning rather than exact keyword matching. Uses vector embeddings.

### Service
Spring component containing business logic. Annotated with `@Service`.

### Similarity Threshold
Minimum cosine similarity score for including a chunk in results. Default: 0.3.

### Sliding Window
Fixed-size window that moves through data stream. Used in circuit breaker to track recent failures.

### SLI (Service Level Indicator)
Quantitative measure of service level (e.g., response time, error rate).

### SLO (Service Level Objective)
Target value for SLI (e.g., 99.9% uptime, < 3s response time).

### Spring Boot
Framework for building production-ready Spring applications with minimal configuration.

### Spring Data JPA
Spring module that simplifies database access using JPA.

### Spring Security
Framework for authentication and authorization in Spring applications.

### SQL (Structured Query Language)
Language for managing relational databases.

### SQL Injection
Attack where malicious SQL code is inserted into queries. Prevented by parameterized queries.

### SSR (Server-Side Rendering)
Rendering web pages on server before sending to client.

### Stateless
Architecture where each request contains all information needed to process it. No session state stored on server.

### Swagger
Tool for API documentation and testing. Accessible at `/swagger-ui/index.html`.

---

## T

### Thread
Smallest unit of execution in a program. Allows concurrent operations.

### Thread Pool
Collection of reusable threads for executing tasks. Configured in `AsyncConfig`.

### Throttling
See **Rate Limiting**.

### Throughput
Number of operations completed per unit time.

### Thundering Herd
Problem where many clients retry simultaneously, overwhelming recovering service. Prevented by jitter.

### Token
1. **Text Token**: Unit of text (~0.75 words). Used for chunking.
2. **Auth Token**: Credential for API authentication.
3. **Rate Limit Token**: Unit consumed per request in token bucket algorithm.

### Token Bucket
Rate limiting algorithm where tokens are added at fixed rate and consumed per request.

### Top-K
Retrieve K most similar results. Default: K=5 chunks.

### Tracing (Distributed)
Tracking requests across multiple services to understand flow and performance.

### Transaction
Set of database operations that execute as single unit (all succeed or all fail).

### TTL (Time To Live)
Duration before cached data expires. Default: 3600 seconds (1 hour).

---

## U

### Uptime
Percentage of time system is operational and available.

---

## V

### Vector
Array of numbers representing data in multi-dimensional space. This project uses 2048-dimensional vectors.

### Vector Database
Database optimized for storing and querying vector embeddings. This project uses PostgreSQL with pgvector.

### Vector Search
Finding similar vectors using distance metrics (cosine, euclidean, etc.).

### Versioning (API)
Managing different versions of API to maintain backward compatibility.

---

## W

### Wait Duration
Time circuit breaker stays in OPEN state before transitioning to HALF_OPEN. Default: 30 seconds.

### WebSocket
Protocol for bidirectional communication between client and server.

---

## X

### XSS (Cross-Site Scripting)
Attack where malicious scripts are injected into web pages. Prevented by CSP headers.

---

## Acronyms Quick Reference

| Acronym | Full Form |
|---------|-----------|
| ADR | Architecture Decision Record |
| AI | Artificial Intelligence |
| API | Application Programming Interface |
| CORS | Cross-Origin Resource Sharing |
| CRUD | Create, Read, Update, Delete |
| CSP | Content Security Policy |
| CSRF | Cross-Site Request Forgery |
| DAO | Data Access Object |
| DTO | Data Transfer Object |
| HNSW | Hierarchical Navigable Small World |
| HTTP | Hypertext Transfer Protocol |
| IoC | Inversion of Control |
| IVFFlat | Inverted File with Flat Compression |
| JAR | Java Archive |
| JPA | Java Persistence API |
| JSON | JavaScript Object Notation |
| JWT | JSON Web Token |
| K8s | Kubernetes |
| LLM | Large Language Model |
| LRU | Least Recently Used |
| NIM | NVIDIA Inference Microservices |
| OAuth | Open Authorization |
| ORM | Object-Relational Mapping |
| PII | Personally Identifiable Information |
| RAG | Retrieval-Augmented Generation |
| REST | Representational State Transfer |
| SLI | Service Level Indicator |
| SLO | Service Level Objective |
| SQL | Structured Query Language |
| SSR | Server-Side Rendering |
| TTL | Time To Live |
| XSS | Cross-Site Scripting |

---

## Project-Specific Terms

### Document Status
- **PENDING**: Document uploaded, waiting for processing
- **PROCESSING**: Currently extracting text and generating embeddings
- **COMPLETED**: Successfully processed and ready for queries
- **FAILED**: Processing failed (see error_message)

### Cache Names
- **queryEmbeddings**: Caches query vectors (2048-dim arrays)
- **chatResponses**: Caches full AI responses with sources

### Circuit Breaker Instances
- **nvidia-api**: Protects NVIDIA API calls (chat and embedding)

### Thread Pools
- **documentProcessingExecutor**: Handles async PDF processing (5-10 threads)

### Vector Operations
- **<=>**: Cosine distance operator in PostgreSQL (1 - cosine_similarity)
- **<->**: Euclidean distance operator (not used in this project)
- **<#>**: Inner product operator (not used in this project)

---

## Common Formulas

### Cosine Similarity
```
similarity = (A · B) / (||A|| × ||B||)

Where:
- A · B = dot product of vectors A and B
- ||A|| = magnitude (length) of vector A
- ||B|| = magnitude (length) of vector B
```

### Cosine Distance
```
distance = 1 - cosine_similarity
```

### Cache Hit Rate
```
hit_rate = cache_hits / (cache_hits + cache_misses)
```

### Error Rate
```
error_rate = failed_requests / total_requests
```

### Thread Pool Utilization
```
utilization = active_threads / max_pool_size
```

---

## Related Documentation

- **[01-BASICS.md](./01-BASICS.md)** - Beginner-friendly explanations of core concepts
- **[02-INTERMEDIATE.md](./02-INTERMEDIATE.md)** - Detailed explanations of async, caching, resilience
- **[03-ADVANCED.md](./03-ADVANCED.md)** - Deep technical details and formulas
- **[04-REFERENCE.md](./04-REFERENCE.md)** - Quick reference for APIs and configuration
- **[PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)** - File and folder organization

---

**Last Updated**: March 31, 2026
