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

