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

