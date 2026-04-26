# Changelog

All notable changes to the Intelligent Memory Preservation & Retrieval System will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **SETUP.bat**: Docker container not starting when it already exists in a stopped (`Exited`) state
  - Replaced unreliable `docker ps | findstr` pipe detection with `docker inspect` for accurate container existence and running-state checks
  - Added `docker compose` (v2) support with automatic fallback to `docker-compose` (v1) for compatibility with modern Docker Desktop installs
  - Replaced fixed-duration `timeout` sleeps with polling loops — Docker Desktop startup waits up to 60s, database readiness waits up to 30s, both proceeding as soon as ready

### Planned
- Multi-user support with authentication
- Image and video memory support
- Voice interface integration
- Mobile applications (iOS/Android)
- Family sharing features
- Advanced analytics dashboard
- Export memories to PDF
- Timeline visualization
- Real-time collaboration

---

## [1.0.0] - 2024-01-15

### Added
- Initial release of IMPRS
- RAG (Retrieval-Augmented Generation) architecture
- PDF document upload and processing
- Text document upload and processing
- Vector embedding generation using NVIDIA NIM API
- Semantic search with pgvector
- Natural language chat interface
- AI-powered response generation
- Document chunking with configurable size and overlap
- Caching layer for embeddings and retrievals
- Circuit breaker pattern for API resilience
- Retry logic with exponential backoff
- Rate limiting (20 requests/second)
- Health check endpoints
- Diagnostic endpoints
- Swagger UI for API documentation
- OpenAPI 3.0 specification
- Comprehensive error handling
- Input validation and sanitization
- Structured logging with SLF4J
- Prometheus metrics export
- Spring Boot Actuator integration
- Docker Compose setup for PostgreSQL
- Database schema with pgvector extension
- IVFFlat indexing for vector search
- HikariCP connection pooling
- Async document processing
- Batch embedding generation
- Source citation in responses
- Similarity threshold configuration
- Top-k retrieval configuration
- Multiple environment profiles (dev, local, prod)
- Windows batch scripts for setup
- Comprehensive documentation
  - README.md
  - API_REFERENCE.md
  - ARCHITECTURE.md
  - DEPLOYMENT.md
  - DEVELOPMENT.md
  - FAQ.md
  - CONTRIBUTING.md
- MIT License
- .gitignore for Java/Maven/Spring Boot
- Environment variable template (.env.example)

### Technical Details
- **Framework**: Spring Boot 3.5.12
- **Language**: Java 17
- **Database**: PostgreSQL 16 with pgvector
- **AI Models**: 
  - NVIDIA NV-Embed-v1 (embeddings)
  - Meta Llama 3.1 70B Instruct (chat)
- **Caching**: Caffeine
- **Resilience**: Resilience4j
- **API Docs**: SpringDoc OpenAPI
- **Build Tool**: Maven
- **Containerization**: Docker

### Performance Optimizations
- Vector search with IVFFlat indexing
- Connection pooling (20 max connections)
- Multi-level caching (embeddings + retrievals)
- Async document processing
- Batch API calls
- Query result caching
- Optimized chunking strategy (800 tokens, 100 overlap)
- Efficient embedding conversion
- Prepared statement caching

### Security Features
- Input validation with Bean Validation
- Input sanitization to prevent prompt injection
- SQL injection prevention via JPA
- Rate limiting per client
- Request size limits (10MB)
- Secure credential management
- CORS configuration
- Error message sanitization
- No sensitive data in logs

---

## Version History

### Version Numbering

We use Semantic Versioning (MAJOR.MINOR.PATCH):
- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### Release Schedule

- **Major releases**: Annually
- **Minor releases**: Quarterly
- **Patch releases**: As needed for critical bugs

---

## Migration Guides

### Upgrading to 1.0.0

This is the initial release. No migration needed.

---

## Deprecation Notices

None at this time.

---

## Known Issues

### Version 1.0.0

1. **Document Deletion**: No API endpoint for deleting documents
   - **Workaround**: Delete directly from database
   - **Planned Fix**: Version 1.1.0

2. **Large File Processing**: Files > 50MB may timeout
   - **Workaround**: Split large files into smaller chunks
   - **Planned Fix**: Version 1.2.0

3. **Concurrent Uploads**: High concurrent uploads may cause connection pool exhaustion
   - **Workaround**: Increase connection pool size
   - **Planned Fix**: Version 1.1.0

4. **Windows Path Handling**: Some Windows paths with special characters may fail
   - **Workaround**: Use forward slashes in paths
   - **Planned Fix**: Version 1.0.1

---

## Breaking Changes

None in version 1.0.0 (initial release).

---

## Contributors

### Version 1.0.0
- Initial development team

---

## Links

- [GitHub Repository](https://github.com/your-org/IMPRS)
- [Documentation](docs/)
- [Issue Tracker](https://github.com/your-org/IMPRS/issues)
- [Release Notes](https://github.com/your-org/IMPRS/releases)

---

## Changelog Categories

### Added
New features and capabilities

### Changed
Changes to existing functionality

### Deprecated
Features that will be removed in future versions

### Removed
Features that have been removed

### Fixed
Bug fixes

### Security
Security improvements and vulnerability fixes

### Performance
Performance improvements and optimizations

### Documentation
Documentation updates and improvements

---

## Future Roadmap

### Version 1.1.0 (Q2 2024)
- User authentication and authorization
- Document deletion API
- Improved concurrent upload handling
- Enhanced error messages
- Performance monitoring dashboard

### Version 1.2.0 (Q3 2024)
- Image memory support
- Audio transcription
- Advanced search filters
- User preferences
- Export to PDF

### Version 2.0.0 (Q4 2024)
- Multi-tenant architecture
- Real-time collaboration
- Mobile applications
- Voice interface
- Family sharing

---

## Support

For questions about changes or upgrades:
- Check the [FAQ](docs/FAQ.md)
- Review [Migration Guides](#migration-guides)
- Open an [issue](https://github.com/your-org/IMPRS/issues)
- Contact support@example.com
