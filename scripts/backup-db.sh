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

