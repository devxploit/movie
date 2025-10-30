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

## Costs

- Hetzner CPX31: €11.90/month (~$13 USD)
- Automatic Backups: €2.38/month (~$2.60 USD)
- **Total: ~$15.60 USD/month**

