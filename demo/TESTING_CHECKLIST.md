# Testing Checklist for Architecture Improvements

## Pre-Flight Checks

### 1. Build Verification
```bash
cd demo
./mvnw clean install
```
**Expected**: Build succeeds with no compilation errors

**What to check**:
- All dependencies download successfully
- No compilation errors
- Tests pass (or are skipped as configured)

---

### 2. Configuration Verification
```bash
# Check application.properties has all required settings
cat src/main/resources/application.properties | grep -E "(resilience4j|management|spring.cache)"
```

**Expected**: Should see Resilience4j, Actuator, and cache configurations

**What to check**:
- Circuit breaker settings present
- Retry configuration present
- Actuator endpoints configured
- Rate limiter settings present

---

### 3. Database Setup
```bash
# Ensure PostgreSQL is running
psql -U postgres -d pdf_rag_db -c "SELECT 1;"
```

**Expected**: Connection successful

**What to check**:
- Database exists
- pgvector extension installed
- Tables created (documents, document_chunks)

---

## Functional Testing

### 4. Application Startup
```bash
cd demo
./mvnw spring-boot:run
```

**Expected**: Application starts without errors

**What to check in logs**:
```
✅ Started DemoApplication in X seconds
✅ Tomcat started on port 8080
✅ No ERROR or WARN messages about missing beans
✅ Circuit breaker registered: nvidia-api
✅ Cache manager initialized
```

**Red flags**:
```
❌ Failed to configure a DataSource
❌ Bean creation exception
❌ Port 8080 already in use
❌ NVIDIA_API_KEY not found
```

---

### 5. Health Check Endpoint
```bash
curl http://localhost:8080/actuator/health
```

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

**What to check**:
- Overall status is UP
- Database component is UP
- No DOWN components

---

### 6. Metrics Endpoint
```bash
curl http://localhost:8080/actuator/metrics
```

**Expected**: List of available metrics including:
```json
{
  "names": [
    "resilience4j.circuitbreaker.calls",
    "resilience4j.retry.calls",
    "cache.gets",
    "cache.puts",
    "http.server.requests",
    ...
  ]
}
```

**What to check**:
- Resilience4j metrics present
- Cache metrics present
- HTTP metrics present

---

### 7. Prometheus Endpoint
```bash
curl http://localhost:8080/actuator/prometheus
```

**Expected**: Prometheus-formatted metrics

**What to check**:
```
✅ resilience4j_circuitbreaker_state{name="nvidia-api"} 0.0
✅ cache_size{cache="queryEmbeddings"} 0.0
✅ http_server_requests_seconds_count
```

---

## Feature Testing

### 8. Async Document Upload (Critical)
```bash
# Upload a PDF (should return immediately)
time curl -X POST http://localhost:8080/api/documents \
  -F "file=@test.pdf" \
  -w "\nTime: %{time_total}s\n"
```

**Expected**:
- Response time: < 1 second
- HTTP Status: 202 Accepted
- Response body:
```json
{
  "documentId": 123,
  "filename": "test.pdf",
  "status": "PENDING",
  "message": "Document upload accepted. Processing asynchronously."
}
```

**What to check**:
- Response is immediate (not 30-120 seconds)
- Status is PENDING (not COMPLETED)
- documentId is returned

**Check logs for**:
```
✅ [UploadController] Document upload accepted - documentId: 123
✅ [PdfProcessingService] Starting async document processing
✅ [PdfProcessingService] Document processing completed
```

---

### 9. Document Status Polling
```bash
# Check processing status (use documentId from previous step)
curl http://localhost:8080/api/documents/123/status
```

**Expected Response** (while processing):
```json
{
  "documentId": 123,
  "status": "PROCESSING",
  "chunksProcessed": 0
}
```

**Expected Response** (after completion):
```json
{
  "documentId": 123,
  "status": "COMPLETED",
  "chunksProcessed": 42
}
```

**What to check**:
- Status transitions: PENDING → PROCESSING → COMPLETED
- chunksProcessed increases over time

---

### 10. Cache Testing (Critical)
```bash
# First query (cache miss - should be slower)
time curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is this document about?"}' \
  -w "\nTime: %{time_total}s\n"

# Second identical query (cache hit - should be much faster)
time curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is this document about?"}' \
  -w "\nTime: %{time_total}s\n"
```

**Expected**:
- First request: 2-3 seconds
- Second request: < 100ms (20-30x faster)

**Check cache metrics**:
```bash
curl http://localhost:8080/actuator/metrics/cache.gets?tag=cache:queryEmbeddings
```

**What to check**:
```json
{
  "name": "cache.gets",
  "measurements": [
    {"statistic": "COUNT", "value": 2.0}
  ],
  "availableTags": [
    {"tag": "result", "values": ["hit", "miss"]}
  ]
}
```

**Check logs for**:
```
✅ First request: "Generating query embedding" (cache miss)
✅ Second request: No "Generating query embedding" (cache hit)
```

---

### 11. Circuit Breaker Testing

#### Test 1: Normal Operation
```bash
# Should work normally
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "test"}'
```

**Expected**: Normal response

**Check circuit breaker state**:
```bash
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state?tag=name:nvidia-api
```

**Expected**: State = 0 (CLOSED - normal operation)

#### Test 2: Simulate Failures
```bash
# Temporarily set invalid API key to trigger failures
# Edit application.properties: nvidia.api.key=invalid-key
# Restart application

# Make 10 requests (should trigger circuit breaker)
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/chat/query \
    -H "Content-Type: application/json" \
    -d '{"query": "test"}' &
done
wait
```

**Expected**:
- First 5-6 requests: Fail with retry attempts
- After threshold: Circuit breaker opens
- Subsequent requests: Fail immediately with fallback response

**Check logs for**:
```
✅ CircuitBreaker 'nvidia-api' recorded a failure
✅ CircuitBreaker 'nvidia-api' changed state from CLOSED to OPEN
✅ Returning fallback response due to circuit breaker
```

**Check circuit breaker state**:
```bash
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state?tag=name:nvidia-api
```

**Expected**: State = 1 (OPEN - circuit breaker tripped)

---

### 12. Retry Testing

**Check retry metrics**:
```bash
curl http://localhost:8080/actuator/metrics/resilience4j.retry.calls?tag=name:nvidia-api
```

**What to check**:
```json
{
  "name": "resilience4j.retry.calls",
  "measurements": [
    {"statistic": "COUNT", "value": X}
  ],
  "availableTags": [
    {"tag": "kind", "values": ["successful_with_retry", "failed_with_retry"]}
  ]
}
```

**Check logs for exponential backoff**:
```
✅ Retry attempt 1 - waiting 1000ms
✅ Retry attempt 2 - waiting 2000ms
✅ Retry attempt 3 - waiting 4000ms
```

---

### 13. Rate Limiting Testing
```bash
# Send 150 requests rapidly (limit is 100/min)
for i in {1..150}; do
  curl -X POST http://localhost:8080/api/chat/query \
    -H "Content-Type: application/json" \
    -d '{"query": "test"}' &
done
wait
```

**Expected**:
- First 100 requests: HTTP 200 OK
- Requests 101-150: HTTP 429 Too Many Requests

**Expected Response** (after limit):
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

**What to check**:
- Rate limiter kicks in after 100 requests
- Requests are blocked, not queued
- Limit resets after 1 minute

---

### 14. Security Headers Testing
```bash
curl -I http://localhost:8080/api/chat/query
```

**Expected Headers**:
```
Content-Security-Policy: default-src 'self'
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
```

**What to check**:
- All security headers present
- CSP policy is restrictive
- XSS protection enabled

---

### 15. CORS Testing
```bash
# Test from allowed origin (localhost)
curl -X OPTIONS http://localhost:8080/api/chat/query \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -v
```

**Expected**:
```
< Access-Control-Allow-Origin: http://localhost:3000
< Access-Control-Allow-Methods: GET,POST,PUT,DELETE
< Access-Control-Allow-Credentials: true
```

**Test from disallowed origin**:
```bash
curl -X OPTIONS http://localhost:8080/api/chat/query \
  -H "Origin: http://evil.com" \
  -H "Access-Control-Request-Method: POST" \
  -v
```

**Expected**: No CORS headers (request blocked)

---

## Performance Testing

### 16. Load Testing (Optional)
```bash
# Install Apache Bench if not available
# apt-get install apache2-utils  # Linux
# brew install httpd  # macOS

# Test 100 concurrent requests
ab -n 100 -c 10 -p query.json -T application/json \
  http://localhost:8080/api/chat/query
```

**query.json**:
```json
{"query": "test"}
```

**What to check**:
- Requests per second
- Mean response time
- Failed requests (should be 0)
- 95th percentile response time

---

### 17. Memory and Thread Pool Monitoring
```bash
# Check JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.threads.live

# Check async executor metrics
curl http://localhost:8080/actuator/metrics/executor.active?tag=name:asyncExecutor
```

**What to check**:
- Memory usage is stable (no leaks)
- Thread count is reasonable (< 50 for low load)
- Async executor has available threads

---

## Integration Testing

### 18. End-to-End Workflow
```bash
# 1. Upload document
RESPONSE=$(curl -X POST http://localhost:8080/api/documents \
  -F "file=@test.pdf" -s)
DOC_ID=$(echo $RESPONSE | jq -r '.documentId')
echo "Document ID: $DOC_ID"

# 2. Wait for processing (poll every 2 seconds)
while true; do
  STATUS=$(curl -s http://localhost:8080/api/documents/$DOC_ID/status | jq -r '.status')
  echo "Status: $STATUS"
  if [ "$STATUS" = "COMPLETED" ]; then
    break
  fi
  sleep 2
done

# 3. Query the document
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is this document about?"}' | jq

# 4. Query again (should be cached)
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is this document about?"}' | jq
```

**Expected**: Complete workflow succeeds

---

## Troubleshooting Guide

### Issue: Application won't start
**Check**:
```bash
# Database connection
psql -U postgres -d pdf_rag_db -c "SELECT 1;"

# Port availability
netstat -an | grep 8080

# Java version
java -version  # Should be 17+
```

### Issue: Circuit breaker always open
**Check**:
```bash
# NVIDIA API key
echo $NVIDIA_API_KEY

# API connectivity
curl -H "Authorization: Bearer $NVIDIA_API_KEY" \
  https://integrate.api.nvidia.com/v1/models
```

### Issue: Cache not working
**Check logs for**:
```
✅ Cache manager initialized
✅ Caffeine cache created: queryEmbeddings
```

**Check metrics**:
```bash
curl http://localhost:8080/actuator/metrics/cache.size?tag=cache:queryEmbeddings
```

### Issue: Async processing not working
**Check logs for**:
```
✅ Async executor initialized
✅ Starting async document processing
```

**Check thread pool**:
```bash
curl http://localhost:8080/actuator/metrics/executor.pool.size?tag=name:asyncExecutor
```

---

## Success Criteria

### ✅ All Tests Pass If:

1. **Build**: Clean build with no errors
2. **Startup**: Application starts in < 30 seconds
3. **Health**: All health checks are UP
4. **Async**: Upload returns in < 1 second with 202 status
5. **Cache**: Second identical query is 20x+ faster
6. **Circuit Breaker**: Opens after threshold failures
7. **Retry**: Retries with exponential backoff
8. **Rate Limit**: Blocks after 100 requests/min
9. **Security**: All security headers present
10. **CORS**: Only allowed origins accepted
11. **Metrics**: All Resilience4j and cache metrics available
12. **E2E**: Complete upload → process → query workflow succeeds

---

## Quick Smoke Test Script

Save this as `smoke-test.sh`:

```bash
#!/bin/bash
set -e

echo "🧪 Running smoke tests..."

# 1. Health check
echo "✓ Testing health endpoint..."
curl -f http://localhost:8080/actuator/health > /dev/null

# 2. Metrics
echo "✓ Testing metrics endpoint..."
curl -f http://localhost:8080/actuator/metrics > /dev/null

# 3. Prometheus
echo "✓ Testing Prometheus endpoint..."
curl -f http://localhost:8080/actuator/prometheus > /dev/null

# 4. Upload (async)
echo "✓ Testing async upload..."
RESPONSE=$(curl -f -X POST http://localhost:8080/api/documents \
  -F "file=@test.pdf" -s)
STATUS=$(echo $RESPONSE | jq -r '.status')
if [ "$STATUS" != "PENDING" ]; then
  echo "❌ Upload should return PENDING status"
  exit 1
fi

# 5. Cache test
echo "✓ Testing cache..."
QUERY='{"query": "test"}'
TIME1=$(curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d "$QUERY" -w "%{time_total}" -o /dev/null -s)
TIME2=$(curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d "$QUERY" -w "%{time_total}" -o /dev/null -s)
echo "  First request: ${TIME1}s"
echo "  Second request: ${TIME2}s (should be much faster)"

echo "✅ All smoke tests passed!"
```

Run with:
```bash
chmod +x smoke-test.sh
./smoke-test.sh
```

---

## Next Steps After Testing

1. **If all tests pass**: Ready for production deployment
2. **If some tests fail**: Check troubleshooting guide above
3. **Before production**: Update CORS origins in SecurityConfig
4. **Set up monitoring**: Configure Prometheus and Grafana
5. **Configure alerts**: Set up alerts for circuit breaker state

---

## Monitoring in Production

### Key Metrics to Watch

```bash
# Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state{name="nvidia-api"}

# Cache hit rate
cache_gets_total{cache="queryEmbeddings",result="hit"} / 
cache_gets_total{cache="queryEmbeddings"}

# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Response time (95th percentile)
histogram_quantile(0.95, http_server_requests_seconds_bucket)
```

### Recommended Alerts

1. Circuit breaker open for > 5 minutes
2. Cache hit rate < 50%
3. Error rate > 5%
4. Response time p95 > 5 seconds
5. Memory usage > 80%

---

**Testing completed successfully? You're ready for production! 🚀**
