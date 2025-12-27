package com.moviesp.builder.services;

import com.moviesp.builder.clients.TMDbApiClient;
import com.moviesp.builder.dtos.tmdb.GenresResponse;
import com.moviesp.builder.entities.GenreEntity;
import com.moviesp.builder.repositories.GenreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenreSyncService {

    private final GenreRepository genreRepository;
    private final WebClient tmdbWebClient;
    private final String apiKey;

    public GenreSyncService(GenreRepository genreRepository,
                           @org.springframework.beans.factory.annotation.Qualifier("tmdbWebClient") WebClient tmdbWebClient,
                           @Value("${tmdb.api.key}") String apiKey) {
        this.genreRepository = genreRepository;
        this.tmdbWebClient = tmdbWebClient;
        this.apiKey = apiKey;
    }

    @Transactional
    public void syncGenresFromTMDB() {
        log.info("Starting genre synchronization from TMDB...");
        
        try {
            // Sync movie genres
            List<GenreEntity> movieGenres = fetchGenresFromTMDB("movie");
            saveGenres(movieGenres, "movie");
            
            // Sync TV show genres
            List<GenreEntity> tvGenres = fetchGenresFromTMDB("tv");
            saveGenres(tvGenres, "tv");
            
            log.info("Genre synchronization completed successfully");
        } catch (Exception e) {
            log.error("Error during genre synchronization: {}", e.getMessage(), e);
            throw e;
        }
    }

    private List<GenreEntity> fetchGenresFromTMDB(String type) {
        try {
            GenresResponse response = tmdbWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/genre/{type}/list")
                            .queryParam("language", "es")
                            .queryParam("api_key", apiKey)
                            .build(type))
                    .retrieve()
                    .bodyToMono(GenresResponse.class)
                    .block();

            if (response != null && response.genres() != null) {
                return response.genres().stream()
                        .map(dto -> GenreEntity.builder()
                                .id((long) dto.id())
                                .name(dto.name())
                                .type(type)
                                .build())
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching {} genres from TMDB: {}", type, e.getMessage(), e);
            throw e;
        }
    }

    private void saveGenres(List<GenreEntity> genres, String type) {
        if (genres.isEmpty()) {
            log.warn("No {} genres to save", type);
            return;
        }

        genres.forEach(genre -> {
            if (!genreRepository.existsByIdAndType(genre.getId(), genre.getType())) {
                genreRepository.save(genre);
                log.debug("Saved {} genre: {} ({})", type, genre.getName(), genre.getId());
            } else {
                log.debug("Genre already exists: {} ({})", genre.getName(), genre.getId());
            }
        });

        log.info("Saved {} {} genres", genres.size(), type);
    }
}

