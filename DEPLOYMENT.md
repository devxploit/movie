# Hetzner Cloud Deployment Guide

Complete deployment guide for deploying the Spring Boot application on Hetzner Cloud CPX31.

## Prerequisites

- Hetzner Cloud account
- Domain names pointed to Hetzner IP
- SSH access to Hetzner server
- Docker installed on local machine (for building React app)

## Quick Start

### 1. Server Setup

Create a CPX31 server in Hetzner Cloud (Ubuntu 22.04 LTS recommended).

### 2. DNS Configuration

Point your domains to the Hetzner server IP:
```
api.yourdomain.com     A    <YOUR_IP>
admin.yourdomain.com   A    <YOUR_IP>
```

### 3. Deploy

```bash
# SSH into server
ssh root@<YOUR_IP>

# Clone repository
git clone https://github.com/your-repo/movie.git
cd movie

# Configure environment
cp .env.example .env
nano .env  # Edit with your values

# Edit SSL domains in init-ssl.sh
nano scripts/init-ssl.sh  # Update domains and email

# Make scripts executable
chmod +x scripts/*.sh

# Upload React build files
# From your local machine:
scp -r dashboard/build/* root@<YOUR_IP>:/root/movie/dashboard/build/

# Run deployment
./scripts/deploy.sh
```

## Services

- **API**: `https://api.yourdomain.com` - Spring Boot API
- **Dashboard**: `https://admin.yourdomain.com` - React Admin Dashboard
- **Grafana**: `http://localhost:3000` - Monitoring Dashboard
- **Prometheus**: `http://localhost:9090` - Metrics Server

## Useful Commands

```bash
# View logs
docker compose logs -f app

# Restart service
docker compose restart app

# Backup database
./scripts/backup-db.sh

# Restore database
./scripts/restore-db.sh backups/moviedb_backup_YYYYMMDD_HHMMSS.sql.gz

# Update application
git pull
docker compose build app
docker compose up -d app

# Check SSL certificates
docker compose exec certbot certbot certificates

# Reload Nginx
docker compose exec nginx nginx -s reload
```

## Environment Variables

Edit `.env` file with:
- `DB_PASSWORD`: PostgreSQL password
- `LEGACY_USER`: Yandex Disk user
- `LEGACY_TOKEN`: Yandex Disk token
- `UPDATES_USER`: Yandex Disk updates user
- `UPDATES_TOKEN`: Yandex Disk updates token
- `GRAFANA_PASSWORD`: Grafana admin password

## SSL Certificates

Certificates are automatically managed by Certbot and renewed every 12 hours.

## Backups

Database backups run automatically at 2 AM daily. Manual backups:
```bash
./scripts/backup-db.sh
```

## Monitoring

- Prometheus metrics: `http://localhost:9090`
- Grafana dashboard: `http://localhost:3000` (admin / password from .env)

## Troubleshooting

### Check service status
```bash
docker compose ps
```

### View all logs
```bash
docker compose logs -f
```

### Check Nginx configuration
```bash
docker compose exec nginx nginx -t
```

### Recreate certificates
```bash
./scripts/init-ssl.sh
```

## Ejecución local con Docker

### Requisitos

- Docker 24.x o superior y Docker Compose v2 (`docker compose version`)
- Al menos 4 GB de RAM para contenedores (API + PostgreSQL + Redis)

### 1. Preparar el entorno

```bash
git clone https://github.com/your-repo/movie.git
cd movie
```

1. Crea un archivo `.env` (Compose lo lee automáticamente) con las variables mínimas:

   ```bash
   cat <<'EOF' > .env
   DB_PASSWORD=superseguro
   LEGACY_USER=
   LEGACY_TOKEN=
   UPDATES_USER=
   UPDATES_TOKEN=
   GRAFANA_PASSWORD=admin
   EOF
   ```

   Ajusta los valores según tus credenciales. El resto de variables (`DATABASE_*`, `REDIS_*`, etc.) ya vienen definidas en `docker-compose.yml`.

2. (Opcional) Construye el dashboard si quieres servirlo a través de Nginx:

   ```bash
   cd dashboard
   npm install
   npm run build
   cd ..
   ```

   El resultado queda en `dashboard/build`, que Nginx montará cuando habilites ese servicio.

### 2. Levantar los servicios básicos

Para desarrollo basta con API + PostgreSQL + Redis. `docker-compose.override.yml` ya expone los puertos ideales (`http://localhost:10000` para la API y `localhost:5432` para PostgreSQL).

```bash
docker compose up -d postgres redis app
```

Compose construirá la imagen de Spring Boot usando el `Dockerfile`, aplicará las migraciones Flyway y expondrá el health check en `http://localhost:10000/actuator/health`.

Verifica:

```bash
curl http://localhost:10000/actuator/health
```

### 3. Servicios opcionales mediante perfiles

- **Nginx + Certbot** (simular proxy/SSL):  
  `docker compose --profile nginx up -d nginx certbot`
- **Prometheus + Grafana** (observabilidad):  
  `docker compose --profile monitoring up -d prometheus grafana`

Los perfiles mantienen liviano el entorno local porque solo inicias los contenedores necesarios.

### 4. Comandos útiles en local

```bash
# Seguir logs de la API
docker compose logs -f app

# Reconstruir la imagen tras cambios en el código
docker compose build app
docker compose up -d app

# Backup manual de la base
./scripts/backup-db.sh
```

### 5. Apagar y limpiar

```bash
# Detener contenedores conservando volúmenes
docker compose down

# Detener y borrar volúmenes (datos de Postgres/Redis)
docker compose down -v
```

Con estos pasos tendrás la plataforma funcionando localmente con Docker para pruebas y desarrollo.

## Costs

- Hetzner CPX31: €11.90/month (~$13 USD)
- Automatic Backups: €2.38/month (~$2.60 USD)
- **Total: ~$15.60 USD/month**

