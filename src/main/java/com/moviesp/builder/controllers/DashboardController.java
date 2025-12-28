package com.moviesp.builder.controllers;

import com.moviesp.builder.entities.MovieEntity;
import com.moviesp.builder.entities.TvShowEntity;
import com.moviesp.builder.repositories.MovieRepository;
import com.moviesp.builder.repositories.TvShowRepository;
import com.moviesp.builder.services.IntegratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final com.moviesp.builder.repositories.GenreRepository genreRepository;
    private final IntegratorService integratorService;
    private final com.moviesp.builder.services.UserService userService;

    // --- MOVIES ---

    @GetMapping("/movies")
    public ResponseEntity<Page<MovieEntity>> getMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MovieEntity> movies;
        if (search != null && !search.isEmpty()) {
            movies = movieRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            movies = movieRepository.findAll(pageable);
        }
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/movies/{id}")
    public ResponseEntity<MovieEntity> getMovie(@PathVariable Long id) {
        return movieRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/movies/{id}")
    public ResponseEntity<MovieEntity> updateMovie(@PathVariable Long id, @RequestBody MovieEntity movieDetails) {
        return movieRepository.findById(id).map(movie -> {
            movie.setName(movieDetails.getName());
            movie.setTmdbId(movieDetails.getTmdbId());
            movie.setPosterPath(movieDetails.getPosterPath());
            movie.setUserScore(movieDetails.getUserScore());
            // Add other fields as necessary
            return ResponseEntity.ok(movieRepository.save(movie));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        if (movieRepository.existsById(id)) {
            movieRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- TV SHOWS ---

    @GetMapping("/tvshows")
    public ResponseEntity<Page<TvShowEntity>> getTvShows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TvShowEntity> tvShows;
        if (search != null && !search.isEmpty()) {
            tvShows = tvShowRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            tvShows = tvShowRepository.findAll(pageable);
        }
        return ResponseEntity.ok(tvShows);
    }

    @PutMapping("/tvshows/{id}")
    public ResponseEntity<TvShowEntity> updateTvShow(@PathVariable Long id, @RequestBody TvShowEntity tvShowDetails) {
        return tvShowRepository.findById(id).map(tvShow -> {
            tvShow.setName(tvShowDetails.getName());
            tvShow.setTmdbId(tvShowDetails.getTmdbId());
            tvShow.setPosterPath(tvShowDetails.getPosterPath());
            tvShow.setUserScore(tvShowDetails.getUserScore());
            // Add other fields as necessary
            return ResponseEntity.ok(tvShowRepository.save(tvShow));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tvshows/{id}")
    public ResponseEntity<Void> deleteTvShow(@PathVariable Long id) {
        if (tvShowRepository.existsById(id)) {
            tvShowRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- GENRES ---

    @GetMapping("/genres")
    public ResponseEntity<Page<com.moviesp.builder.entities.GenreEntity>> getGenres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(genreRepository.findAll(pageable));
    }

    // --- SYNC ---

    @PostMapping("/sync/first-data")
    public ResponseEntity<String> triggerFirstData(@RequestParam(defaultValue = "legacy") String generator) {
        // Run in a separate thread to avoid blocking the response
        // In a real app, use @Async or a Job queue
        new Thread(() -> {
            log.info("Manually triggered firstData sync via Dashboard for generator: {}", generator);
            integratorService.firstData(generator);
        }).start();
        return ResponseEntity.ok("First Data Sync triggered successfully in background for " + generator);
    }

    @PostMapping("/sync/import")
    public ResponseEntity<String> importFolder(
            @RequestParam String path,
            @RequestParam String type,
            @RequestParam(defaultValue = "legacy") String generator) {
        new Thread(() -> {
            integratorService.importFolder(path, type, generator);
        }).start();
        return ResponseEntity.ok("Import triggered in background for: " + path);
    }

    @PostMapping("/sync/update-urls")
    public ResponseEntity<String> triggerUpdateUrls(@RequestParam(defaultValue = "updates") String generator) {
        new Thread(() -> {
            integratorService.getNewUrls(generator);
        }).start();
        return ResponseEntity.ok("Update URLs scan triggered in background for generator: " + generator);
    }

    // --- USERS ---

    @GetMapping("/users")
    public ResponseEntity<Page<com.moviesp.builder.entities.UserEntity>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.getUsers(search, pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<com.moviesp.builder.entities.UserEntity> getUser(@PathVariable Long id) {
        return userService.getUser(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    public ResponseEntity<com.moviesp.builder.entities.UserEntity> createUser(
            @RequestBody com.moviesp.builder.entities.UserEntity user) {
        try {
            return ResponseEntity.ok(userService.createUser(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<com.moviesp.builder.entities.UserEntity> updateUser(@PathVariable Long id,
            @RequestBody com.moviesp.builder.entities.UserEntity user) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // --- EXPLORER ---

    private final com.moviesp.builder.services.explorer.FolderExplorerService folderExplorerService;

    @GetMapping("/explorer")
    public ResponseEntity<java.util.List<com.moviesp.builder.services.explorer.FolderExplorerService.ExplorerItem>> listItems(
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(defaultValue = "legacy") String generator) {
        return ResponseEntity.ok(folderExplorerService.listItems(path, generator));
    }
}
