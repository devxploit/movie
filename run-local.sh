#!/bin/bash

# Script para ejecutar la aplicación localmente
# Requiere: PostgreSQL y Redis corriendo localmente

set -e

echo "🚀 Iniciando aplicación localmente..."

# Cargar variables de entorno desde .env si existe
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Variables de entorno cargadas desde .env"
else
    echo "⚠️  Archivo .env no encontrado, usando valores por defecto"
    export DB_PASSWORD=${DB_PASSWORD:-superseguro}
    export LEGACY_USER=${LEGACY_USER:-}
    export LEGACY_TOKEN=${LEGACY_TOKEN:-}
    export UPDATES_USER=${UPDATES_USER:-}
    export UPDATES_TOKEN=${UPDATES_TOKEN:-}
fi

# Verificar que PostgreSQL y Redis estén corriendo
echo "🔍 Verificando servicios..."

if ! pg_isready -h localhost -p 5432 -U movieuser > /dev/null 2>&1; then
    echo "❌ PostgreSQL no está corriendo en localhost:5432"
    echo "💡 Inicia PostgreSQL o usa Docker: docker compose up -d postgres redis"
    exit 1
fi

if ! redis-cli -h localhost -p 6379 ping > /dev/null 2>&1; then
    echo "❌ Redis no está corriendo en localhost:6379"
    echo "💡 Inicia Redis o usa Docker: docker compose up -d redis"
    exit 1
fi

echo "✅ Servicios verificados"

# Ejecutar la aplicación usando Docker (más confiable)
echo "🐳 Ejecutando aplicación usando Docker..."
docker compose up -d postgres redis
docker compose build app
docker compose up app

