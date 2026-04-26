# Contributing to IMPRS

Thank you for your interest in contributing to the Intelligent Memory Preservation & Retrieval System! This document provides guidelines and instructions for contributing.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [How to Contribute](#how-to-contribute)
4. [Development Workflow](#development-workflow)
5. [Coding Standards](#coding-standards)
6. [Testing Requirements](#testing-requirements)
7. [Documentation](#documentation)
8. [Pull Request Process](#pull-request-process)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of:
- Age, body size, disability, ethnicity, gender identity and expression
- Level of experience, education, socio-economic status
- Nationality, personal appearance, race, religion
- Sexual identity and orientation

### Our Standards

**Positive behaviors:**
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what's best for the community
- Showing empathy towards others

**Unacceptable behaviors:**
- Harassment, trolling, or insulting comments
- Public or private harassment
- Publishing others' private information
- Other conduct inappropriate in a professional setting

### Enforcement

Violations can be reported to the project maintainers. All complaints will be reviewed and investigated promptly and fairly.

---

## Getting Started

### Prerequisites

Before contributing, ensure you have:
- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Git
- A GitHub account
- NVIDIA API key (for testing)

### Setup Development Environment

1. **Fork the repository**
   ```bash
   # Click "Fork" on GitHub
   git clone https://github.com/YOUR_USERNAME/IMPRS.git
   cd IMPRS
   ```

2. **Add upstream remote**
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/IMPRS.git
   ```

3. **Install dependencies**
   ```bash
   cd demo
   ./mvnw clean install
   ```

4. **Start development database**
   ```bash
   docker-compose up -d
   ```

5. **Run the application**
   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=dev
   ```

6. **Verify setup**
   ```bash
   curl http://localhost:8080/api/health
   ```

---

## How to Contribute

### Types of Contributions

We welcome various types of contributions:

#### 🐛 Bug Reports
- Use the bug report template
- Include steps to reproduce
- Provide system information
- Attach relevant logs

#### ✨ Feature Requests
- Use the feature request template
- Explain the use case
- Describe expected behavior
- Consider implementation approach

#### 📝 Documentation
- Fix typos or unclear sections
- Add examples or tutorials
- Improve API documentation
- Translate documentation

#### 🔧 Code Contributions
- Bug fixes
- New features
- Performance improvements
- Refactoring
- Test coverage improvements

#### 🎨 Design Contributions
- UI/UX improvements
- API design
- Architecture proposals

---

## Development Workflow

### 1. Create an Issue

Before starting work:
1. Check if an issue already exists
2. Create a new issue if needed
3. Discuss your approach with maintainers
4. Wait for approval before starting

### 2. Create a Branch

```bash
# Update your fork
git checkout develop
git pull upstream develop

# Create feature branch
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b bugfix/issue-number-description
```

### Branch Naming Convention

- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical production fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `test/` - Test improvements

### 3. Make Changes

Follow these guidelines:
- Write clean, readable code
- Follow existing code style
- Add tests for new functionality
- Update documentation
- Keep commits atomic and focused

### 4. Commit Changes

Use [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Format
<type>(<scope>): <subject>

# Types
feat:     New feature
fix:      Bug fix
docs:     Documentation changes
style:    Code style changes (formatting)
refactor: Code refactoring
test:     Adding or updating tests
chore:    Maintenance tasks

# Examples
git commit -m "feat(chat): add multi-turn conversation support"
git commit -m "fix(embedding): resolve cache invalidation issue"
git commit -m "docs(api): update endpoint documentation"
```

### 5. Push Changes

```bash
git push origin feature/your-feature-name
```

### 6. Create Pull Request

1. Go to your fork on GitHub
2. Click "New Pull Request"
3. Select your branch
4. Fill out the PR template
5. Link related issues
6. Request review from maintainers

---

## Coding Standards

### Java Style Guide

#### Code Formatting

```java
// Use 4 spaces for indentation
public class MyClass {
    private String field;
    
    public void myMethod() {
        if (condition) {
            // Code here
        }
    }
}
```

#### Naming Conventions

```java
// Classes: PascalCase
public class ChatService { }

// Methods: camelCase
public void processQuery() { }

// Variables: camelCase
private String queryText;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRIES = 3;
```

#### Documentation

```java
/**
 * Process a user query and generate an AI response.
 * 
 * @param query User's natural language query
 * @return ChatResponse with answer and source citations
 * @throws IllegalArgumentException if query is null or empty
 */
public ChatResponse processQuery(String query) {
    // Implementation
}
```

### Spring Boot Best Practices

```java
// ✅ Use constructor injection
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyRepository repository;
}

// ✅ Use @Slf4j for logging
@Slf4j
public class MyService {
    public void process() {
        log.info("Processing started");
    }
}

// ✅ Use proper exception handling
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MyException.class)
    public ResponseEntity<ErrorResponse> handle(MyException ex) {
        // Handle exception
    }
}
```

---

## Testing Requirements

### Test Coverage

All contributions must include tests:
- **New features**: 90%+ coverage
- **Bug fixes**: Test for the bug + regression tests
- **Refactoring**: Maintain existing coverage

### Writing Tests

#### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    
    @Mock
    private MyRepository repository;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void myMethod_withValidInput_returnsExpectedResult() {
        // Arrange
        when(repository.find()).thenReturn(data);
        
        // Act
        Result result = service.myMethod();
        
        // Assert
        assertNotNull(result);
        verify(repository).find();
    }
}
```

#### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class MyControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void endpoint_withValidRequest_returns200() throws Exception {
        mockMvc.perform(post("/api/endpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());
    }
}
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=MyServiceTest

# Run with coverage
./mvnw test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

---

## Documentation

### What to Document

- **Code**: JavaDoc for public APIs
- **API**: Update OpenAPI annotations
- **Features**: Add to README.md
- **Configuration**: Document new properties
- **Architecture**: Update ARCHITECTURE.md if needed

### Documentation Standards

```java
/**
 * Brief description of what this does.
 * 
 * More detailed explanation if needed. Can span
 * multiple lines and include examples.
 * 
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 * @see RelatedClass
 * @since 1.1.0
 */
public ReturnType methodName(ParamType paramName) {
    // Implementation
}
```

---

## Pull Request Process

### Before Submitting

Checklist:
- [ ] Code follows style guidelines
- [ ] Tests pass locally (`./mvnw test`)
- [ ] New tests added for new functionality
- [ ] Documentation updated
- [ ] Commit messages follow convention
- [ ] No merge conflicts with develop
- [ ] Self-review completed

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issues
Closes #123

## Testing
Describe testing performed

## Checklist
- [ ] Tests pass
- [ ] Documentation updated
- [ ] Code reviewed
```

### Review Process

1. **Automated Checks**
   - CI/CD pipeline runs
   - Tests must pass
   - Code coverage checked
   - Linting verified

2. **Code Review**
   - At least one approval required
   - Address all comments
   - Make requested changes

3. **Merge**
   - Squash and merge to develop
   - Delete feature branch
   - Close related issues

### After Merge

- Monitor CI/CD pipeline
- Verify in development environment
- Update issue status
- Thank reviewers!

---

## Development Tips

### Debugging

```bash
# Debug mode
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Attach debugger to port 5005
```

### Hot Reload

```bash
# Enable Spring Boot DevTools
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Database Access

```bash
# Connect to development database
docker exec -it rag-postgres psql -U raguser ragdb

# View tables
\dt

# View schema
\d document_chunks
```

---

## Getting Help

### Resources

- **Documentation**: [docs/](docs/)
- **API Reference**: [docs/API_REFERENCE.md](docs/API_REFERENCE.md)
- **Architecture**: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Development Guide**: [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)
- **FAQ**: [docs/FAQ.md](docs/FAQ.md)

### Communication

- **GitHub Issues**: For bugs and features
- **GitHub Discussions**: For questions and ideas
- **Pull Requests**: For code review
- **Email**: For private matters

### Response Times

- **Issues**: 1-3 business days
- **Pull Requests**: 2-5 business days
- **Security Issues**: 24 hours

---

## Recognition

Contributors will be:
- Listed in CONTRIBUTORS.md
- Mentioned in release notes
- Credited in documentation

Thank you for contributing to IMPRS! 🎉

---

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
