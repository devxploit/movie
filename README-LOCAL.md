# Ejecución Local del Proyecto

## Problema con Java 25

Si tienes Java 25 instalado y encuentras el error `Unsupported class file major version 69`, esto se debe a que Gradle 8.10/8.14 tiene problemas de compatibilidad con Java 25.

## Soluciones

### Opción 1: Usar Docker (Recomendado) ✅

La forma más fácil y confiable de ejecutar el proyecto localmente es usando Docker:

```bash
# 1. Asegúrate de tener las variables de entorno en .env
cat .env

# 2. Inicia los servicios
docker compose up -d postgres redis app

# 3. Verifica que esté corriendo
curl http://localhost:10000/health

# 4. Ejecuta el endpoint
curl http://localhost:10000/firstData

# 5. Ver logs
docker compose logs -f app
```

### Opción 2: Instalar Java 21

Si necesitas ejecutar sin Docker, instala Java 21:

```bash
# macOS con Homebrew
brew install openjdk@21

# Configurar JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH

# Verificar versión
java -version  # Debe mostrar Java 21

# Compilar y ejecutar
./gradlew clean build
./gradlew bootRun
```

### Opción 3: Usar el script de ejecución

```bash
./run-local.sh
```

## Configuración del IDE

### VS Code / Cursor

1. Instala la extensión "Extension Pack for Java"
2. Configura el Java Runtime:
   - Abre Command Palette (Cmd+Shift+P)
   - Busca "Java: Configure Java Runtime"
   - Selecciona Java 21 si está disponible

### IntelliJ IDEA

1. File → Project Structure → Project
2. SDK: Selecciona Java 21
3. Language level: 21

## Variables de Entorno Requeridas

Crea un archivo `.env` en la raíz del proyecto:

```bash
DB_PASSWORD=superseguro
LEGACY_USER=tu_usuario_yandex
LEGACY_TOKEN=tu_token_yandex
UPDATES_USER=tu_usuario_updates
UPDATES_TOKEN=tu_token_updates
GRAFANA_PASSWORD=admin
```

## Servicios Requeridos

- **PostgreSQL**: Puerto 5432
- **Redis**: Puerto 6379

Puedes iniciarlos con Docker:
```bash
docker compose up -d postgres redis
```

## Endpoints Disponibles

- `GET /health` - Health check
- `GET /firstData` - Ejecuta el proceso de integración

## Ver Logs

```bash
# Docker
docker compose logs -f app

# Local (si funciona)
./gradlew bootRun
```

