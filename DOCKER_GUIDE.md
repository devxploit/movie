# 🐳 Guía de Gestión con Docker

Esta documentación detalla cómo levantar, gestionar y mantener el entorno de la aplicación utilizando Docker y Docker Compose.

## 📋 Requisitos Previos

- [Docker](https://docs.docker.com/get-docker/) instalado y ejecutándose.
- [Docker Compose](https://docs.docker.com/compose/install/) (generalmente incluido con Docker Desktop).

## 🚀 Inicio Rápido

Para levantar la aplicación básica (Backend, Frontend, Base de Datos y Redis) en entorno de desarrollo:

```bash
docker compose up -d --build
```

Esto iniciará los contenedores y expondrá los siguientes servicios localmente (gracias a `docker-compose.override.yml`):

- **API Backend**: [http://localhost:10000](http://localhost:10000)
- **Frontend**: [http://localhost:3000](http://localhost:3000)
- **PostgreSQL**: Puerto `5432`
- **Redis**: Puerto `6379`

## 🎛️ Perfiles (Profiles)

El archivo `docker-compose.override.yml` configura perfiles para servicios opcionales. Puedes activarlos según necesites:

### 1. Perfil Completo (Todo incluido)
```bash
docker compose --profile nginx --profile monitoring up -d
```

### 2. Solo Nginx (Proxy Reverso y SSL)
```bash
docker compose --profile nginx up -d
```
Esto levantará el contenedor de `nginx` y `certbot`. Útil para pruebas de integración o producción.

### 3. Solo Monitoreo (Prometheus y Grafana)
```bash
docker compose --profile monitoring up -d
```
Esto levantará:
- **Prometheus**: Recolección de métricas.
- **Grafana**: Dashboard visual.

## �️ Gestión Operativa con Docker Compose

Esta sección detalla los comandos esenciales para administrar el ciclo de vida de la aplicación.

### 1. Estado y Monitoreo

**Ver contenedores activos:**
Muestra el estado (Up/Exit), puertos y nombres.
```bash
docker compose ps
```

**Ver logs en tiempo real:**
Sigue el output (`-f`) de todos los servicios o uno específico.
```bash
# Todos los servicios
docker compose logs -f

# Solo backend y logs de errores
docker compose logs -f app | grep ERROR
```

**Monitorizar consumo de recursos:**
Ver CPU y Memoria en tiempo real.
```bash
docker stats
```

### 2. Ciclo de Vida de Contenedores

**Iniciar servicios (Background):**
Si hay cambios en `Dockerfile` o `docker-compose.yml`, añade `--build`.
```bash
docker compose up -d
# O forzando reconstrucción:
docker compose up -d --build
```

**Detener servicios (Stop):**
Detiene los contenedores pero mantiene el estado.
```bash
docker compose stop
```

**Iniciar servicios detenidos (Start):**
```bash
docker compose start
```

**Reiniciar servicios:**
Útil tras cambiar configuración o si un servicio se bloquea.
```bash
docker compose restart app
```

### 3. Mantenimiento y Limpieza

**Apagar y Eliminar (Down):**
Detiene y elimina contenedores y redes. **No borra volúmenes**.
```bash
docker compose down
```

**Limpieza Profunda (Destructivo):**
Detiene todo y **BORRA LOS DATOS** (Volúmenes de BD, Redis, etc.).
```bash
docker compose down -v
```

### 4. Interacción con Contenedores

**Ejecutar comandos dentro de un contenedor:**
Acceder a la shell de un contenedor (ej. backend).
```bash
docker compose exec app sh
# O bash si está disponible
docker compose exec app bash
```

**Ejecutar comandos de base de datos:**
Conectarse directamente a PostgreSQL sin instalar cliente local.
```bash
docker compose exec postgres psql -U movieuser -d moviedb
```

**Verificar conectividad:**
Probar si el backend ve la base de datos.
```bash
docker compose exec app nc -zv postgres 5432
```

## 📜 Scripts de Utilidad

El proyecto incluye scripts en la carpeta `scripts/` para facilitar tareas comunes de mantenimiento y despliegue.

### 1. Backups de Base de Datos (`scripts/backup-db.sh`)
Genera un backup comprimido de la base de datos PostgreSQL.
- Guarda los archivos en `./backups`.
- Mantiene solo los últimos 7 días de backups.

```bash
./scripts/backup-db.sh
```

### 2. Restaurar Base de Datos (`scripts/restore-db.sh`)
Restaura un backup específico.

```bash
./scripts/restore-db.sh backups/moviedb_backup_20231227_120000.sql.gz
```

### 3. Configuración SSL (`scripts/init-ssl.sh`)
Gestiona la obtención y renovación de certificados SSL Let's Encrypt para Nginx.
- Edita el script para configurar tus dominios y email.
- Ejecuta automáticamente pasos para solicitar certificados.

```bash
./scripts/init-ssl.sh
```

### 4. Despliegue (`scripts/deploy.sh`)
Script completo para desplegar en un servidor nuevo (ej. Ubuntu/Debian). instala Docker, configura firewall y levanta la app.

## 🏗️ Estructura de Servicios

| Servicio | Nombre Contenedor | Descripción | Puerto Interno | Puerto Host (Dev) |
|----------|-------------------|-------------|----------------|-------------------|
| **app** | `moviesp-api` | Backend Spring Boot (Java 21) | 8080 | 10000 |
| **frontend**| `moviesp-frontend`| Frontend Next.js | 3000 | 3000 |
| **postgres**| `moviesp-postgres`| Base de datos PostgreSQL 16 | 5432 | 5432 |
| **redis** | `moviesp-redis` | Cache Redis 7 | 6379 | 6379 |
| **nginx** | `nginx-proxy` | Proxy reverso (Perfil `nginx`) | 80, 443 | 80, 443 |
| **grafana** | `grafana` | Monitoreo (Perfil `monitoring`) | 3000 | - |

## ⚙️ Configuración y Variables de Entorno

Las variables principales están definidas en `docker-compose.yml`. Para producción o personalización local, puedes crear un archivo `.env` en la raíz (aunque `docker-compose` ya tiene valores por defecto).

Variables clave:
- `DB_PASSWORD`: Contraseña de la BD (Default: `superseguro`).
- `PORT`: Puerto interno de la app (Default: `8080`).
- `GRAFANA_PASSWORD`: Password admin Grafana (Default: `admin`).

## 💾 Persistencia de Datos (Volúmenes)

Los datos se persisten en volúmenes de Docker para sobrevivir a reinicios de contenedores:

- `postgres_data`: Datos de la base de datos `/var/lib/postgresql/data`.
- `redis_data`: Datos de Redis `/data`.
- `prometheus_data`: Histórico de métricas.
- `grafana_data`: Dashboards y configuraciones de Grafana.

Para encontrar dónde están estos volúmenes en tu disco:
```bash
docker volume ls
docker volume inspect movie_postgres_data
```

## 🩺 Healthchecks

Los servicios críticos tienen healthchecks configurados:
- **App**: Verifica `/actuator/health`.
- **Postgres**: Usa `pg_isready`.
- **Redis**: Usa `redis-cli ping`.

Si un servicio dependiente no inicia, Docker esperará a que el servicio "healthy" esté listo antes de arrancar el siguiente.


docker compose up -d --build frontend