<!-- ab5a3027-3a73-4408-a6a0-fac2442c9627 a7657ebd-0419-489b-9f27-8845ddd73f3f -->
# Migración de Cloudflare D1 a PostgreSQL

## Estructura de Datos

Migraremos 4 tablas principales de D1 a un esquema relacional normalizado en PostgreSQL:

**Tablas principales:**

- `folders` - Control de carpetas procesadas
- `genres` - Géneros de películas/series
- `movies` - Películas con relaciones a géneros y videos
- `tvshows` - Series con relaciones a géneros, temporadas y episodios

**Tablas relacionales (normalización):**

- `movie_genres` - Relación many-to-many movies ↔ genres
- `movie_aliases` - Aliases de películas
- `movie_videos` - Videos de películas
- `tvshow_genres` - Relación many-to-many tvshows ↔ genres
- `tvshow_aliases` - Aliases de series
- `seasons` - Temporadas de series
- `episodes` - Episodios de temporadas
- `episode_videos` - Videos de episodios

## Implementación

### 1. Migración Flyway - Nueva Estructura

**Archivo:** `src/main/resources/db/migration/V2__add_movies_and_tvshows_tables.sql`

Crear todas las tablas relacionales normalizadas reemplazando la tabla movies existente.

### 2. Entidades JPA

**Crear en:** `src/main/java/com/moviesp/builder/entities/`

- `FolderEntity.java` - Mapea tabla folders
- `GenreEntity.java` - Mapea tabla genres con relaciones
- `MovieEntity.java` - Mapea tabla movies con relaciones @OneToMany
- `MovieAliasEntity.java` - Aliases de películas
- `MovieVideoEntity.java` - Videos de películas
- `TvShowEntity.java` - Mapea tabla tvshows con relaciones @OneToMany
- `TvShowAliasEntity.java` - Aliases de series
- `SeasonEntity.java` - Temporadas con relación @ManyToOne a TvShow
- `EpisodeEntity.java` - Episodios con relación @ManyToOne a Season
- `EpisodeVideoEntity.java` - Videos de episodios

**Anotaciones clave:** `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@JoinTable`

### 3. Repositorios Spring Data JPA

**Crear en:** `src/main/java/com/moviesp/builder/repositories/`

- `FolderRepository.java` - extends JpaRepository<FolderEntity, Long>
  - Métodos: `findByPathAndUser()`, `existsByPathAndUser()`
- `GenreRepository.java` - extends JpaRepository<GenreEntity, Long>
  - Métodos: `findByIdAndType()`, `saveAll()` para batch
- `MovieRepository.java` - extends JpaRepository<MovieEntity, Long>
  - Métodos: `findByTmdbId()`, `saveAll()` para batch
- `TvShowRepository.java` - extends JpaRepository<TvShowEntity, Long>
  - Métodos: `findByTmdbId()`, `saveAll()` para batch

### 4. Servicios de Persistencia

**Crear:** `src/main/java/com/moviesp/builder/services/DatabaseService.java`

Servicio principal que replica la funcionalidad de CloudflareWorkerApiClient:

```java
@Service
@Transactional
public class DatabaseService {
    
    // Inyectar todos los repositorios
    
    public void saveMovie(Movie movie) { }
    public void saveMovies(List<Movie> movies) { }
    public void saveTvShow(TvShow tvShow) { }
    public void saveTvShows(List<TvShow> tvShows) { }
    public List<Folder> getFolders() { }
    public void updateFolder(String path, String status, String user) { }
    
    // Métodos helper para conversión DTO → Entity
}
```

**Crear:** `src/main/java/com/moviesp/builder/services/GenreSyncService.java`

Servicio para sincronizar géneros desde TMDB:

```java
@Service
public class GenreSyncService {
    public void syncGenresFromTMDB() { }
    private void saveGenres(List<GenreDto> genres, String type) { }
}
```

### 5. Actualizar IntegratorService

**Archivo:** `src/main/java/com/moviesp/builder/services/IntegratorService.java`

Mantener lógica dual temporalmente:

- Línea 45-46: Cambiar `cloudflareWorkerApiClient.getFolders()` → `databaseService.getFolders()`
- Línea 68: Agregar llamada a `databaseService.updateFolder()` ADEMÁS de cloudflare
- Línea 74: Agregar llamada a `databaseService.updateFolder()` ADEMÁS de cloudflare
- Línea 103: Agregar llamada a `databaseService.saveMovies()` ADEMÁS de cloudflare
- Línea 129: Agregar llamada a `databaseService.saveTvShows()` ADEMÁS de cloudflare

### 6. Mappers DTO ↔ Entity

**Crear:** `src/main/java/com/moviesp/builder/mappers/`

- `MovieMapper.java` - Conversión entre Movie DTO y MovieEntity
- `TvShowMapper.java` - Conversión entre TvShow DTO y TvShowEntity
- `FolderMapper.java` - Conversión entre Folder DTO y FolderEntity

Métodos estáticos para conversión bidireccional con manejo de relaciones.

### 7. Actualizar DTOs

**Archivo:** `src/main/java/com/moviesp/builder/dtos/Movie.java`

- Agregar campo `posterPath` (actualmente falta)

**Archivo:** `src/main/java/com/moviesp/builder/dtos/TvShow.java`

- Agregar campos `posterPath`, `userScore` (actualmente faltan)

### 8. Script de Sincronización Inicial de Géneros

**Crear:** Endpoint o CommandLineRunner para ejecutar una vez:

```java
@Component
public class GenreSyncRunner implements CommandLineRunner {
    @Override
    public void run(String... args) {
        // Sincronizar géneros desde TMDB una sola vez
    }
}
```

## Orden de Implementación

1. Crear migración V2 con todas las tablas
2. Crear todas las entidades JPA con anotaciones correctas
3. Crear todos los repositorios
4. Crear mappers DTO ↔ Entity
5. Crear DatabaseService con todos los métodos
6. Crear GenreSyncService
7. Actualizar DTOs faltantes (Movie, TvShow)
8. Actualizar IntegratorService para escritura dual
9. Crear GenreSyncRunner para sincronización inicial

## Pruebas Finales

- Ejecutar aplicación y verificar que Flyway crea las tablas
- Ejecutar sincronización de géneros
- Ejecutar proceso de integración y verificar:
  - Datos se escriben en PostgreSQL
  - Datos se escriben en Cloudflare (dual write)
  - Consultas de folders funcionan desde PostgreSQL
  - No hay errores de mapeo o conversión

### To-dos

- [x] Crear migración Flyway V2__add_movies_and_tvshows_tables.sql con todas las tablas relacionales
- [x] Crear todas las entidades JPA (Folder, Genre, Movie, TvShow, Season, Episode, Videos, Aliases)
- [x] Crear repositorios Spring Data JPA para todas las entidades
- [x] Crear mappers para conversión DTO ↔ Entity
- [x] Crear DatabaseService con métodos para persistir movies, tvshows y folders
- [x] Crear GenreSyncService para sincronizar géneros desde TMDB
- [x] Actualizar DTOs Movie y TvShow con campos faltantes (posterPath, userScore)
- [x] Actualizar IntegratorService para escritura dual (PostgreSQL + Cloudflare)
- [x] Crear CommandLineRunner para sincronización inicial de géneros