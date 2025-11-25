# Ejecución local con Docker

Guía paso a paso para levantar el proyecto `movie` en tu máquina usando Docker Compose.

## 1. Prerrequisitos

- Docker Engine 24.x o superior y Docker Compose v2 (`docker compose version`)
- Al menos 4 GB de RAM libres para los contenedores (API + PostgreSQL + Redis)
- Puertos `10000` y `5432` disponibles en tu host

## 2. Clonar el repositorio

```bash
git clone https://github.com/your-repo/movie.git
cd movie
```

## 3. Configurar variables de entorno

Compose lee automáticamente un archivo `.env` ubicado en la raíz del proyecto. Crea uno con los valores mínimos requeridos:

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

- `DB_PASSWORD` se usa para PostgreSQL y para que la API se conecte a la base.
- Las credenciales `LEGACY_*` y `UPDATES_*` son necesarias solo si consumes los orígenes externos asociados.
- `GRAFANA_PASSWORD` aplica cuando habilitas el perfil de monitoreo.

> El resto de variables (`DATABASE_*`, `REDIS_*`, etc.) ya están definidas en `docker-compose.yml`.

## 4. Levantar los servicios básicos

Para desarrollo basta con la API, PostgreSQL y Redis. El archivo `docker-compose.override.yml` ya expone los puertos ideales (`http://localhost:10000` para la API y `localhost:5432` para la base de datos).

```bash
docker compose up -d postgres redis app
```

El proceso construirá la imagen de Spring Boot a partir del `Dockerfile`, aplicará las migraciones de Flyway y dejará un _health check_ disponible en `http://localhost:10000/actuator/health`.

### Verificar que todo funciona

```bash
curl http://localhost:10000/actuator/health
docker compose ps
```

## 5. Servicios opcionales mediante perfiles

Los perfiles permiten iniciar solo la infraestructura que necesitas:

- **Proxy + SSL (perfil `nginx`)**  
  `docker compose --profile nginx up -d nginx certbot`
- **Monitoreo (perfil `monitoring`)**  
  `docker compose --profile monitoring up -d prometheus grafana`

Si deseas servir el dashboard React a través de Nginx, compila primero:

```bash
cd dashboard
npm install
npm run build
cd ..
```

Nginx montará el resultado desde `dashboard/build`.

## 6. Comandos útiles en local

```bash
# Seguir logs de la API
docker compose logs -f app

# Reconstruir imagen tras cambios en el código
docker compose build app
docker compose up -d app

# Crear un respaldo manual de la base
./scripts/backup-db.sh
```

## 7. Apagar y limpiar

```bash
# Detener contenedores conservando volúmenes
docker compose down

# Detener y borrar volúmenes (datos de PostgreSQL/Redis)
docker compose down -v
```

## 8. Solución de problemas rápida

- **La API no arranca**: revisa `docker compose logs app` y confirma que PostgreSQL está _healthy_.  
- **Puertos en uso**: cambia los puertos en `docker-compose.override.yml` o libera los existentes.  
- **Cambios en la base no se aplican**: elimina el volumen `postgres_data` (`docker volume rm movie_postgres_data`) para recrear la base desde cero.  
- **Variables faltantes**: ejecuta `docker compose config` para verificar que Compose está cargando tu `.env`.

Con esto deberías tener el entorno local completamente funcional usando Docker.

