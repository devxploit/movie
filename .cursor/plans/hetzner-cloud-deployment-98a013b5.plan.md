<!-- 98a013b5-3274-4936-bf45-5f8b4b7c4290 54227068-c346-4191-b5ce-3c28b89cb522 -->
# Despliegue Completo en Hetzner Cloud CPX31

## Descripción General

Desplegar aplicación Spring Boot con stack completo containerizado en Hetzner Cloud CPX31, incluyendo:

- Spring Boot API (puerto 8080 interno)
- Dashboard React (archivos estáticos servidos por Nginx)
- PostgreSQL 16 con Flyway migrations
- Redis 7 para caché
- Nginx como reverse proxy con SSL (Let's Encrypt)
- Prometheus + Grafana para monitoreo

Dominios: `api.yourdomain.com` y `admin.yourdomain.com` con certificados SSL automáticos.

## Archivos a Crear/Modificar

### 1. Configuración de Base de Datos

**build.gradle** - Agregar dependencias de PostgreSQL, Redis y Flyway:

```gradle
dependencies {
    // Existing dependencies...
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.postgresql:postgresql'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
}
```

**src/main/resources/application.properties** - Agregar configuración de DB y Redis:

```properties
# Database
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/moviedb}
spring.datasource.username=${DATABASE_USER:movieuser}
spring.datasource.password=${DATABASE_PASSWORD:changeme}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.cache.type=redis
```

**src/main/resources/db/migration/V1__initial_schema.sql** - Crear esquema inicial (ejemplo):

```sql
CREATE TABLE IF NOT EXISTS movies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    tmdb_id INTEGER UNIQUE,
    overview TEXT,
    release_date DATE,
    poster_path VARCHAR(500),
    backdrop_path VARCHAR(500),
    vote_average DECIMAL(3,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movies_tmdb_id ON movies(tmdb_id);
CREATE INDEX idx_movies_release_date ON movies(release_date);
```

### 2. Docker Compose Stack Completo

**docker-compose.yml** - Stack completo con todos los servicios:

```yaml
version: '3.8'

services:
  # Nginx Reverse Proxy con SSL
  nginx:
    image: nginx:alpine
    container_name: nginx-proxy
    ports:
   - "80:80"
   - "443:443"
    volumes:
   - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
   - ./nginx/conf.d:/etc/nginx/conf.d:ro
   - ./certbot/conf:/etc/letsencrypt:ro
   - ./certbot/www:/var/www/certbot:ro
   - ./dashboard/build:/usr/share/nginx/html/dashboard:ro
    depends_on:
   - app
    networks:
   - app-network
    restart: unless-stopped

  # Certbot para SSL
  certbot:
    image: certbot/certbot
    container_name: certbot
    volumes:
   - ./certbot/conf:/etc/letsencrypt
   - ./certbot/www:/var/www/certbot
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done;'"
    networks:
   - app-network

  # Spring Boot API
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: moviesp-api
    environment:
   - PORT=8080
   - JAVA_OPTS=-Xms512m -Xmx1536m
   - DATABASE_URL=jdbc:postgresql://postgres:5432/moviedb
   - DATABASE_USER=movieuser
   - DATABASE_PASSWORD=${DB_PASSWORD}
   - REDIS_HOST=redis
   - REDIS_PORT=6379
   - LEGACY_USER=${LEGACY_USER}
   - LEGACY_TOKEN=${LEGACY_TOKEN}
   - UPDATES_USER=${UPDATES_USER}
   - UPDATES_TOKEN=${UPDATES_TOKEN}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
   - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: moviesp-postgres
    environment:
   - POSTGRES_DB=moviedb
   - POSTGRES_USER=movieuser
   - POSTGRES_PASSWORD=${DB_PASSWORD}
   - PGDATA=/var/lib/postgresql/data/pgdata
    volumes:
   - postgres_data:/var/lib/postgresql/data
   - ./backups:/backups
    networks:
   - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U movieuser -d moviedb"]
      interval: 10s
      timeout: 5s
      retries: 5
    command: >
      postgres
      -c max_connections=100
      -c shared_buffers=256MB
      -c effective_cache_size=1GB
      -c maintenance_work_mem=128MB
      -c checkpoint_completion_target=0.9
      -c wal_buffers=16MB
      -c default_statistics_target=100

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: moviesp-redis
    command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru --appendonly yes
    volumes:
   - redis_data:/data
    networks:
   - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  # Prometheus Monitoring
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    volumes:
   - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
   - prometheus_data:/prometheus
    command:
   - '--config.file=/etc/prometheus/prometheus.yml'
   - '--storage.tsdb.path=/prometheus'
   - '--storage.tsdb.retention.time=30d'
    networks:
   - app-network
    restart: unless-stopped

  # Grafana Dashboard
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    environment:
   - GF_SECURITY_ADMIN_USER=admin
   - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
   - GF_SERVER_ROOT_URL=https://monitoring.yourdomain.com
   - GF_INSTALL_PLUGINS=redis-datasource
    volumes:
   - grafana_data:/var/lib/grafana
   - ./grafana/provisioning:/etc/grafana/provisioning:ro
    depends_on:
   - prometheus
    networks:
   - app-network
    restart: unless-stopped

networks:
  app-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:
```

### 3. Nginx Configuration

**nginx/nginx.conf** - Configuración principal:

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 2048;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    client_max_body_size 20M;

    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript 
               application/json application/javascript application/xml+rss 
               application/rss+xml font/truetype font/opentype 
               application/vnd.ms-fontobject image/svg+xml;

    include /etc/nginx/conf.d/*.conf;
}
```

**nginx/conf.d/api.conf** - Configuración API con SSL:

```nginx
# HTTP redirect to HTTPS
server {
    listen 80;
    server_name api.yourdomain.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS API
server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;
    
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    location / {
        proxy_pass http://app:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /actuator/health {
        proxy_pass http://app:8080/actuator/health;
        access_log off;
    }
}
```

**nginx/conf.d/dashboard.conf** - Configuración Dashboard con SSL:

```nginx
# HTTP redirect to HTTPS
server {
    listen 80;
    server_name admin.yourdomain.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS Dashboard
server {
    listen 443 ssl http2;
    server_name admin.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/admin.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.yourdomain.com/privkey.pem;
    
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    root /usr/share/nginx/html/dashboard;
    index index.html index.htm;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### 4. Prometheus Configuration

**prometheus/prometheus.yml**:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
 - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
   - targets: ['app:8080']
        labels:
          application: 'moviesp-api'

 - job_name: 'prometheus'
    static_configs:
   - targets: ['localhost:9090']

 - job_name: 'postgres'
    static_configs:
   - targets: ['postgres:5432']

 - job_name: 'redis'
    static_configs:
   - targets: ['redis:6379']
```

### 5. Variables de Entorno

**.env** - Archivo de variables sensibles (NO commitear):

```bash
# Database
DB_PASSWORD=your_secure_db_password_here

# Yandex API
LEGACY_USER=your_legacy_user
LEGACY_TOKEN=your_legacy_token
UPDATES_USER=your_updates_user
UPDATES_TOKEN=your_updates_token

# Grafana
GRAFANA_PASSWORD=your_secure_grafana_password
```

**.env.example** - Template público:

```bash
DB_PASSWORD=changeme
LEGACY_USER=
LEGACY_TOKEN=
UPDATES_USER=
UPDATES_TOKEN=
GRAFANA_PASSWORD=changeme
```

### 6. Scripts de Deployment

**scripts/deploy.sh** - Script de despliegue inicial:

```bash
#!/bin/bash
set -e

echo "=== Deployment Script para Hetzner Cloud ==="

# 1. Actualizar sistema
echo "1. Actualizando sistema..."
sudo apt update && sudo apt upgrade -y

# 2. Instalar Docker
echo "2. Instalando Docker..."
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 3. Instalar Docker Compose
echo "3. Instalando Docker Compose..."
sudo apt install docker-compose-plugin -y

# 4. Crear estructura de directorios
echo "4. Creando estructura de directorios..."
mkdir -p nginx/conf.d
mkdir -p certbot/conf certbot/www
mkdir -p prometheus grafana/provisioning
mkdir -p backups dashboard/build

# 5. Configurar firewall
echo "5. Configurando firewall..."
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

# 6. Obtener certificados SSL (primera vez)
echo "6. Obteniendo certificados SSL..."
./scripts/init-ssl.sh

# 7. Construir y levantar servicios
echo "7. Iniciando servicios..."
docker compose build
docker compose up -d

echo "=== Deployment completado ==="
echo "API: https://api.yourdomain.com"
echo "Dashboard: https://admin.yourdomain.com"
echo "Grafana: https://monitoring.yourdomain.com"
```

**scripts/init-ssl.sh** - Inicializar certificados SSL:

```bash
#!/bin/bash
set -e

domains=(api.yourdomain.com admin.yourdomain.com)
email="your-email@example.com"
staging=0 # Set to 1 for testing

echo "=== Inicializando certificados SSL Let's Encrypt ==="

# Create dummy certificates
for domain in "${domains[@]}"; do
  path="/etc/letsencrypt/live/$domain"
  mkdir -p "./certbot/conf/live/$domain"
  
  if [ ! -f "./certbot/conf/live/$domain/fullchain.pem" ]; then
    echo "### Creating dummy certificate for $domain ..."
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
      -keyout "./certbot/conf/live/$domain/privkey.pem" \
      -out "./certbot/conf/live/$domain/fullchain.pem" \
      -subj "/CN=localhost"
  fi
done

# Start nginx
echo "### Starting nginx ..."
docker compose up -d nginx

# Delete dummy certificates
for domain in "${domains[@]}"; do
  echo "### Removing dummy certificate for $domain ..."
  docker compose run --rm --entrypoint "\
    rm -rf /etc/letsencrypt/live/$domain && \
    rm -rf /etc/letsencrypt/archive/$domain && \
    rm -rf /etc/letsencrypt/renewal/$domain.conf" certbot
done

# Request certificates
for domain in "${domains[@]}"; do
  echo "### Requesting Let's Encrypt certificate for $domain ..."
  
  if [ $staging != "0" ]; then staging_arg="--staging"; fi
  
  docker compose run --rm --entrypoint "\
    certbot certonly --webroot -w /var/www/certbot \
      $staging_arg \
      --email $email \
      --agree-tos \
      --no-eff-email \
      -d $domain" certbot
done

# Reload nginx
echo "### Reloading nginx ..."
docker compose exec nginx nginx -s reload

echo "=== SSL certificates configured successfully ==="
```

**scripts/backup-db.sh** - Backup automático de PostgreSQL:

```bash
#!/bin/bash
set -e

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/moviedb_backup_$TIMESTAMP.sql.gz"

echo "=== Iniciando backup de PostgreSQL ==="

# Crear backup
docker compose exec -T postgres pg_dump -U movieuser moviedb | gzip > $BACKUP_FILE

echo "Backup creado: $BACKUP_FILE"

# Mantener solo últimos 7 backups
find $BACKUP_DIR -name "moviedb_backup_*.sql.gz" -type f -mtime +7 -delete

echo "=== Backup completado ==="
```

**scripts/restore-db.sh** - Restaurar backup:

```bash
#!/bin/bash
set -e

if [ -z "$1" ]; then
  echo "Uso: ./scripts/restore-db.sh <backup_file.sql.gz>"
  exit 1
fi

BACKUP_FILE=$1

echo "=== Restaurando backup: $BACKUP_FILE ==="

gunzip -c $BACKUP_FILE | docker compose exec -T postgres psql -U movieuser -d moviedb

echo "=== Restauración completada ==="
```

### 7. Monitoring Setup

**grafana/provisioning/datasources/prometheus.yml**:

```yaml
apiVersion: 1

datasources:
 - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

### 8. Actualizar Dockerfile para Métricas

**Dockerfile** - Agregar soporte para métricas Prometheus:

```dockerfile
# Build stage (Gradle + JDK21)
FROM gradle:8-jdk21 AS build
WORKDIR /home/gradle/project

# Copiar archivos necesarios
COPY --chown=gradle:gradle . .

# Build (con wrapper o gradle)
RUN gradle clean bootJar -x test

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Instalar wget para healthcheck
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1536m"
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT} -Dserver.address=0.0.0.0 -jar /app/app.jar"]
```

### 9. Crontab para Backups Automáticos

```bash
# Agregar a crontab: crontab -e
0 2 * * * /path/to/project/scripts/backup-db.sh >> /var/log/db-backup.log 2>&1
```

## Instrucciones de Despliegue

### En Hetzner Cloud:

1. **Crear servidor CPX31**:

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - Region: Nuremberg o Ashburn
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - OS: Ubuntu 22.04 LTS
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - SSH Key: agregar tu clave pública

2. **Configurar DNS** (antes de desplegar):
   ```
   api.yourdomain.com     A    <IP_HETZNER>
   admin.yourdomain.com   A    <IP_HETZNER>
   ```

3. **Conectar por SSH y clonar repositorio**:
   ```bash
   ssh root@<IP_HETZNER>
   git clone https://github.com/your-repo/movie.git
   cd movie
   ```

4. **Configurar variables de entorno**:
   ```bash
   cp .env.example .env
   nano .env  # Editar con valores reales
   ```

5. **Editar scripts con tus dominios**:
   ```bash
   # En scripts/init-ssl.sh, cambiar:
   domains=(api.yourdomain.com admin.yourdomain.com)
   email="tu-email@ejemplo.com"
   ```

6. **Hacer ejecutables los scripts**:
   ```bash
   chmod +x scripts/*.sh
   ```

7. **Colocar build del dashboard React**:
   ```bash
   # Copiar archivos build de React a dashboard/build/
   # Desde tu máquina local:
   scp -r dashboard/build/* root@<IP_HETZNER>:/root/movie/dashboard/build/
   ```

8. **Ejecutar deployment**:
   ```bash
   ./scripts/deploy.sh
   ```

9. **Verificar servicios**:
   ```bash
   docker compose ps
   docker compose logs -f app
   ```

10. **Configurar backup automático**:
    ```bash
    crontab -e
    # Agregar línea para backup diario a las 2 AM
    ```


### Comandos Útiles:

```bash
# Ver logs de todos los servicios
docker compose logs -f

# Reiniciar un servicio específico
docker compose restart app

# Actualizar aplicación
git pull
docker compose build app
docker compose up -d app

# Ver métricas
curl http://localhost:9090  # Prometheus
# Grafana en navegador: https://monitoring.yourdomain.com

# Backup manual
./scripts/backup-db.sh

# Verificar SSL
openssl s_client -connect api.yourdomain.com:443 -servername api.yourdomain.com
```

## Estructura Final del Proyecto

```
movie/
├── docker-compose.yml
├── Dockerfile
├── .env
├── .env.example
├── nginx/
│   ├── nginx.conf
│   └── conf.d/
│       ├── api.conf
│       └── dashboard.conf
├── certbot/
│   ├── conf/
│   └── www/
├── prometheus/
│   └── prometheus.yml
├── grafana/
│   └── provisioning/
│       └── datasources/
│           └── prometheus.yml
├── dashboard/
│   └── build/  (archivos React compilados)
├── backups/
├── scripts/
│   ├── deploy.sh
│   ├── init-ssl.sh
│   ├── backup-db.sh
│   └── restore-db.sh
├── src/
│   └── main/
│       └── resources/
│           └── db/
│               └── migration/
│                   └── V1__initial_schema.sql
└── build.gradle
```

## Costos Finales

- Hetzner CPX31: €11.90/mes (~$13 USD)
- Backups automáticos: €2.38/mes (~$2.60 USD)
- **Total: ~$15.60 USD/mes**

## Notas de Seguridad

- Cambiar todas las contraseñas en `.env`
- No commitear archivo `.env` (ya en .gitignore)
- Configurar firewall UFW (solo puertos 22, 80, 443)
- SSL automático con renovación cada 12 horas
- Backups diarios automáticos
- Logs centralizados en `/var/log/nginx/`

### To-dos

- [x] Agregar dependencias de PostgreSQL, Redis y Flyway a build.gradle
- [x] Actualizar application.properties con configuración de database, Redis y Flyway
- [x] Crear directorio db/migration y archivo V1__initial_schema.sql con esquema inicial
- [x] Crear docker-compose.yml con todos los servicios (nginx, app, postgres, redis, prometheus, grafana, certbot)
- [x] Crear nginx.conf y archivos de configuración para API y dashboard con SSL
- [x] Crear prometheus.yml y configuración de datasource para Grafana
- [x] Crear archivos .env.example y .env con variables de entorno necesarias
- [x] Crear scripts de deployment: deploy.sh, init-ssl.sh, backup-db.sh, restore-db.sh
- [x] Actualizar Dockerfile para incluir wget, healthcheck y métricas de Prometheus
- [x] Asegurar que .env, certbot/, backups/ estén en .gitignore