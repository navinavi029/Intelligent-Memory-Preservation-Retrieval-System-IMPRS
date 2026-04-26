# Deployment Guide

Complete guide for deploying the Intelligent Memory Preservation & Retrieval System to various environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development](#local-development)
3. [Docker Deployment](#docker-deployment)
4. [Production Deployment](#production-deployment)
5. [Cloud Deployment](#cloud-deployment)
6. [Configuration Management](#configuration-management)
7. [Monitoring Setup](#monitoring-setup)
8. [Backup and Recovery](#backup-and-recovery)

---

## Prerequisites

### Required Software

- **Java 17 or higher**
  ```bash
  java -version
  # Should show: openjdk version "17.0.x" or higher
  ```

- **Maven 3.6+**
  ```bash
  mvn -version
  # Should show: Apache Maven 3.6.x or higher
  ```

- **Docker & Docker Compose**
  ```bash
  docker --version
  docker-compose --version
  ```

- **PostgreSQL 16** (if not using Docker)

### Required Credentials

- **NVIDIA API Key**: Get from [https://build.nvidia.com](https://build.nvidia.com)

---

## Local Development

### 1. Clone and Setup

```bash
# Clone repository
git clone <repository-url>
cd IMPRS

# Copy environment template
cp .env.example .env

# Edit .env and add your NVIDIA API key
nano .env  # or use your preferred editor
```

### 2. Start Database

**Option A: Using Docker (Recommended)**
```bash
docker-compose up -d
```

**Option B: Using Windows Batch Script**
```bash
SETUP.bat
```

**Option C: Manual PostgreSQL Setup**
```bash
# Install PostgreSQL 16 with pgvector
# Then run the setup script
psql -U postgres -f demo/setup-db.sql
```

### 3. Verify Database

```bash
# Check if container is running
docker ps | grep rag-postgres

# Connect to database
docker exec -it rag-postgres psql -U raguser -d ragdb

# Verify pgvector extension
ragdb=# \dx
# Should show: vector | 0.5.1 | public | vector data type and ivfflat access method
```

### 4. Build Application

```bash
cd demo
./mvnw clean install
```

### 5. Run Application

**Development Mode:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**With Custom JVM Options:**
```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xmx2g -Xms512m" \
  -Dspring-boot.run.profiles=dev
```

### 6. Verify Deployment

```bash
# Health check
curl http://localhost:8080/api/health

# Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

---

## Docker Deployment

### 1. Create Dockerfile

Create `demo/Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
    "-jar", \
    "app.jar"]
```

### 2. Update docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: rag-postgres
    environment:
      POSTGRES_DB: ragdb
      POSTGRES_USER: raguser
      POSTGRES_PASSWORD: ${DB_PASSWORD:-ragpass}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./demo/setup-db.sql:/docker-entrypoint-initdb.d/setup-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U raguser -d ragdb"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - imprs-network

  app:
    build:
      context: ./demo
      dockerfile: Dockerfile
    container_name: imprs-app
    environment:
      SPRING_PROFILES_ACTIVE: prod
      NVIDIA_API_KEY: ${NVIDIA_API_KEY}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ragdb
      SPRING_DATASOURCE_USERNAME: raguser
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-ragpass}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - imprs-network
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:

networks:
  imprs-network:
    driver: bridge
```

### 3. Build and Run

```bash
# Build images
docker-compose build

# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### 4. Verify Deployment

```bash
# Check running containers
docker-compose ps

# Test API
curl http://localhost:8080/api/health

# View application logs
docker-compose logs -f app

# View database logs
docker-compose logs -f postgres
```

---

## Production Deployment

### 1. Prepare Production Build

```bash
cd demo

# Build with production profile
./mvnw clean package -DskipTests -Pprod

# Verify JAR
ls -lh target/*.jar
```

### 2. Create Production Configuration

Create `demo/src/main/resources/application-prod.properties`:

```properties
# Server Configuration
server.port=8080
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain

# Database Configuration
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10

# JPA Configuration
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=validate

# Logging
logging.level.root=WARN
logging.level.com.example.demo=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Actuator
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=never

# Security
server.error.include-message=never
server.error.include-stacktrace=never
```

### 3. Create Systemd Service

Create `/etc/systemd/system/imprs.service`:

```ini
[Unit]
Description=Intelligent Memory Preservation System
After=network.target postgresql.service

[Service]
Type=simple
User=imprs
Group=imprs
WorkingDirectory=/opt/imprs
ExecStart=/usr/bin/java \
    -Xms1g \
    -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Dspring.profiles.active=prod \
    -jar /opt/imprs/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=imprs

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/imprs/logs

[Install]
WantedBy=multi-user.target
```

### 4. Deploy Application

```bash
# Create application user
sudo useradd -r -s /bin/false imprs

# Create application directory
sudo mkdir -p /opt/imprs/logs
sudo chown -R imprs:imprs /opt/imprs

# Copy JAR file
sudo cp target/intelligent-memory-preservation-system-1.0.0.jar /opt/imprs/app.jar

# Create environment file
sudo nano /opt/imprs/.env
# Add:
# NVIDIA_API_KEY=your-key
# DATABASE_URL=jdbc:postgresql://localhost:5432/ragdb
# DB_USERNAME=raguser
# DB_PASSWORD=secure-password

# Set permissions
sudo chmod 600 /opt/imprs/.env
sudo chown imprs:imprs /opt/imprs/.env

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable imprs
sudo systemctl start imprs

# Check status
sudo systemctl status imprs

# View logs
sudo journalctl -u imprs -f
```

### 5. Setup Nginx Reverse Proxy

Create `/etc/nginx/sites-available/imprs`:

```nginx
upstream imprs_backend {
    server localhost:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name your-domain.com;

    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Logging
    access_log /var/log/nginx/imprs_access.log;
    error_log /var/log/nginx/imprs_error.log;

    # Rate Limiting
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
    limit_req zone=api_limit burst=20 nodelay;

    # Proxy Configuration
    location / {
        proxy_pass http://imprs_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # File Upload Size
    client_max_body_size 10M;
}
```

Enable site:
```bash
sudo ln -s /etc/nginx/sites-available/imprs /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## Cloud Deployment

### AWS Deployment

#### 1. RDS PostgreSQL Setup

```bash
# Create RDS instance with pgvector
aws rds create-db-instance \
    --db-instance-identifier imprs-db \
    --db-instance-class db.t3.medium \
    --engine postgres \
    --engine-version 16.1 \
    --master-username raguser \
    --master-user-password <secure-password> \
    --allocated-storage 100 \
    --storage-type gp3 \
    --vpc-security-group-ids sg-xxxxx \
    --db-subnet-group-name imprs-subnet-group \
    --backup-retention-period 7 \
    --preferred-backup-window "03:00-04:00" \
    --preferred-maintenance-window "mon:04:00-mon:05:00"

# Enable pgvector extension
psql -h <rds-endpoint> -U raguser -d ragdb -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

#### 2. Elastic Beanstalk Deployment

Create `.ebextensions/01_pgvector.config`:

```yaml
commands:
  01_install_pgvector:
    command: |
      psql $DATABASE_URL -c "CREATE EXTENSION IF NOT EXISTS vector;"
    ignoreErrors: true
```

Deploy:
```bash
# Initialize EB
eb init -p java-17 imprs-app

# Create environment
eb create imprs-prod \
    --instance-type t3.medium \
    --envvars NVIDIA_API_KEY=$NVIDIA_API_KEY,DATABASE_URL=$DATABASE_URL

# Deploy
eb deploy

# Open application
eb open
```

#### 3. ECS Fargate Deployment

Create `task-definition.json`:

```json
{
  "family": "imprs-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "imprs-app",
      "image": "<account-id>.dkr.ecr.<region>.amazonaws.com/imprs:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "NVIDIA_API_KEY",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:nvidia-api-key"
        },
        {
          "name": "DATABASE_URL",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:database-url"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/imprs",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### Google Cloud Platform

#### 1. Cloud SQL Setup

```bash
# Create Cloud SQL instance
gcloud sql instances create imprs-db \
    --database-version=POSTGRES_16 \
    --tier=db-custom-2-7680 \
    --region=us-central1

# Enable pgvector
gcloud sql connect imprs-db --user=postgres
postgres=> CREATE EXTENSION vector;
```

#### 2. Cloud Run Deployment

```bash
# Build and push image
gcloud builds submit --tag gcr.io/PROJECT_ID/imprs

# Deploy to Cloud Run
gcloud run deploy imprs \
    --image gcr.io/PROJECT_ID/imprs \
    --platform managed \
    --region us-central1 \
    --allow-unauthenticated \
    --set-env-vars SPRING_PROFILES_ACTIVE=prod \
    --set-secrets NVIDIA_API_KEY=nvidia-api-key:latest \
    --set-cloudsql-instances PROJECT_ID:us-central1:imprs-db \
    --memory 2Gi \
    --cpu 2
```

### Azure Deployment

#### 1. Azure Database for PostgreSQL

```bash
# Create PostgreSQL server
az postgres flexible-server create \
    --resource-group imprs-rg \
    --name imprs-db \
    --location eastus \
    --admin-user raguser \
    --admin-password <secure-password> \
    --sku-name Standard_D2s_v3 \
    --version 16

# Enable pgvector
az postgres flexible-server parameter set \
    --resource-group imprs-rg \
    --server-name imprs-db \
    --name azure.extensions \
    --value vector
```

#### 2. App Service Deployment

```bash
# Create App Service plan
az appservice plan create \
    --name imprs-plan \
    --resource-group imprs-rg \
    --sku P1V2 \
    --is-linux

# Create web app
az webapp create \
    --resource-group imprs-rg \
    --plan imprs-plan \
    --name imprs-app \
    --runtime "JAVA:17-java17"

# Configure app settings
az webapp config appsettings set \
    --resource-group imprs-rg \
    --name imprs-app \
    --settings \
        SPRING_PROFILES_ACTIVE=prod \
        NVIDIA_API_KEY=@Microsoft.KeyVault(SecretUri=...) \
        DATABASE_URL=jdbc:postgresql://imprs-db.postgres.database.azure.com:5432/ragdb

# Deploy JAR
az webapp deploy \
    --resource-group imprs-rg \
    --name imprs-app \
    --src-path target/intelligent-memory-preservation-system-1.0.0.jar \
    --type jar
```

---

## Configuration Management

### Environment Variables

**Required:**
- `NVIDIA_API_KEY`: NVIDIA NIM API key
- `DATABASE_URL`: PostgreSQL connection string
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

**Optional:**
- `SPRING_PROFILES_ACTIVE`: Active profile (dev/prod)
- `SERVER_PORT`: Server port (default: 8080)
- `JAVA_OPTS`: JVM options

### Secrets Management

**AWS Secrets Manager:**
```bash
# Store secret
aws secretsmanager create-secret \
    --name imprs/nvidia-api-key \
    --secret-string "nvapi-xxxxx"

# Retrieve in application
export NVIDIA_API_KEY=$(aws secretsmanager get-secret-value \
    --secret-id imprs/nvidia-api-key \
    --query SecretString \
    --output text)
```

**HashiCorp Vault:**
```bash
# Store secret
vault kv put secret/imprs nvidia_api_key="nvapi-xxxxx"

# Retrieve in application
export NVIDIA_API_KEY=$(vault kv get -field=nvidia_api_key secret/imprs)
```

---

## Monitoring Setup

### Prometheus Configuration

Create `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'imprs'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana Dashboard

Import dashboard ID: `4701` (JVM Micrometer)

Custom queries:
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Response time p95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

---

## Backup and Recovery

### Database Backup

```bash
# Manual backup
docker exec rag-postgres pg_dump -U raguser ragdb > backup_$(date +%Y%m%d).sql

# Automated backup script
#!/bin/bash
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
docker exec rag-postgres pg_dump -U raguser ragdb | gzip > $BACKUP_DIR/ragdb_$DATE.sql.gz

# Keep only last 7 days
find $BACKUP_DIR -name "ragdb_*.sql.gz" -mtime +7 -delete
```

### Database Restore

```bash
# Restore from backup
gunzip < backup_20240115.sql.gz | docker exec -i rag-postgres psql -U raguser ragdb
```

### Application State

No application state to backup - all data in database.
