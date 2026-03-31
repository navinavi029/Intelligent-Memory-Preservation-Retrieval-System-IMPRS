# Glossary

## A

**Actuator**: Spring Boot module providing production-ready features like health checks, metrics, and monitoring endpoints.

**ADR (Architecture Decision Record)**: Document capturing important architectural decisions, their context, and consequences.

**API Key**: Secret token for authenticating with NVIDIA NIM services.

**Async Processing**: Executing tasks in background threads without blocking the main request thread.

## B

**Batch Processing**: Processing multiple items together for efficiency (e.g., generating embeddings for 100 chunks at once).

**Bucket4j**: Java library for rate limiting using token bucket algorithm.

## C

**Caffeine**: High-performance, near-optimal caching library for Java.

**Chunking**: Splitting large documents into smaller, overlapping pieces for processing.

**Circuit Breaker**: Design pattern that prevents cascading failures by stopping calls to failing services.

**Cosine Similarity**: Measure of similarity between two vectors, ranging from -1 to 1. Used for finding similar text chunks.

**CORS (Cross-Origin Resource Sharing)**: Security mechanism controlling which domains can access your API.

**CSRF (Cross-Site Request Forgery)**: Attack where unauthorized commands are transmitted from a user the web application trusts.

## D

**DTO (Data Transfer Object)**: Object carrying data between processes, typically for API requests/responses.

**Durable Execution**: Execution that can survive failures and resume from where it left off.

## E

**Embedding**: Vector representation of text in high-dimensional space (2048 dimensions in this project).

**Exponential Backoff**: Retry strategy where wait time doubles after each failure (1s, 2s, 4s, ...).

## F

**Fallback**: Alternative response provided when primary service fails (used with circuit breaker).

## H

**HNSW (Hierarchical Navigable Small World)**: Graph-based algorithm for approximate nearest neighbor search. Limited to 2000 dimensions in pgvector.

**HTTP 202 Accepted**: Status code indicating request accepted for processing but not yet completed.

## I

**IVFFlat (Inverted File with Flat Compression)**: Indexing method for vector similarity search. Used in this project because it supports 2048 dimensions.

## J

**JPA (Java Persistence API)**: Java specification for object-relational mapping.

## L

**LLM (Large Language Model)**: AI model trained on vast amounts of text (e.g., Llama 3.1).

**Lombok**: Java library reducing boilerplate code through annotations (@Data, @Getter, etc.).

## M

**Maven**: Build automation and dependency management tool for Java.

**Micrometer**: Application metrics facade supporting multiple monitoring systems.

## N

**NVIDIA NIM (NVIDIA Inference Microservices)**: Cloud API for running NVIDIA AI models.

## O

**Overlap**: Number of tokens shared between consecutive chunks (default: 50).

**ORM (Object-Relational Mapping)**: Technique for converting between incompatible type systems (objects ↔ database tables).

## P

**PDFBox**: Apache library for working with PDF documents.

**pgvector**: PostgreSQL extension for vector similarity search.

**Polling**: Repeatedly checking status until operation completes.

**Prometheus**: Open-source monitoring and alerting toolkit.

## Q

**Query Embedding**: Vector representation of user's question, used for similarity search.

## R

**RAG (Retrieval-Augmented Generation)**: AI technique combining information retrieval with text generation.

**Rate Limiter**: Component controlling the rate of requests to prevent overload.

**Resilience4j**: Java library for building resilient applications (circuit breaker, retry, rate limiter).

**Retry**: Automatically attempting failed operations again.

## S

**Semantic Search**: Finding information based on meaning rather than exact keyword matches.

**Spring AI**: Framework for building AI-powered applications with Spring Boot.

**Spring Boot**: Framework for building production-ready Spring applications with minimal configuration.

**Spring Data JPA**: Spring module simplifying database access with JPA.

**Spring Security**: Framework for authentication and authorization in Spring applications.

**Sliding Window**: Fixed-size window of recent events used for circuit breaker decisions.

## T

**Thread Pool**: Collection of worker threads for executing tasks asynchronously.

**Token**: Unit of text for LLMs (roughly 0.75 words in English).

**Top-K**: Retrieving the K most similar results (default: 5).

**TTL (Time To Live)**: Duration before cached data expires (default: 1 hour).

## V

**Vector**: Array of numbers representing text in high-dimensional space.

**Vector Database**: Database optimized for storing and searching vectors.

**Vector Similarity Search**: Finding vectors closest to a query vector using distance metrics.

## W

**WebFlux**: Spring's reactive web framework (not used in this project).

## References
- Spring Boot: https://spring.io/projects/spring-boot
- Resilience4j: https://resilience4j.readme.io/
- pgvector: https://github.com/pgvector/pgvector
- NVIDIA NIM: https://www.nvidia.com/en-us/ai-data-science/products/nim/
