package com.moviesp.builder.mappers;

import com.moviesp.builder.dtos.Genre;
import com.moviesp.builder.dtos.Movie;
import com.moviesp.builder.dtos.Video;
import com.moviesp.builder.entities.*;
import com.moviesp.builder.repositories.GenreRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MovieMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static MovieEntity toEntity(Movie movie, GenreRepository genreRepository) {
        MovieEntity entity = MovieEntity.builder()
                .name(movie.getTitle())
                .date(movie.getDate() != null ? LocalDate.parse(movie.getDate(), DATE_FORMATTER) : null)
                .tmdbId(movie.getId() != null ? Long.parseLong(movie.getId()) : null)
                .posterPath(movie.getPosterPath())
                .userScore(movie.getUserScore() != null ? BigDecimal.valueOf(movie.getUserScore()) : null)
                .genres(mapGenres(movie.getGenres(), genreRepository))
                .aliases(mapAliases(movie.getAliases()))
                .videos(mapVideos(movie.getVideos()))
                .build();

        // Set back references
        if (entity.getAliases() != null) {
            entity.getAliases().forEach(alias -> alias.setMovie(entity));
        }
        if (entity.getVideos() != null) {
            entity.getVideos().forEach(video -> video.setMovie(entity));
        }

        return entity;
    }

    public static Movie toDto(MovieEntity entity) {
        return Movie.builder()
                .id(entity.getTmdbId() != null ? entity.getTmdbId().toString() : null)
                .date(entity.getDate() != null ? entity.getDate().format(DATE_FORMATTER) : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FORMATTER) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(FORMATTER) : null)
                .title(entity.getName())
                .genres(mapGenreDtos(entity.getGenres()))
                .userScore(entity.getUserScore() != null ? entity.getUserScore().doubleValue() : null)
                .aliases(mapAliasStrings(entity.getAliases()))
                .videos(mapVideoDtos(entity.getVideos()))
                .build();
    }

    private static Set<GenreEntity> mapGenres(java.util.List<Genre> genres, GenreRepository genreRepository) {
        if (genres == null || genres.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return genres.stream()
                .map(genre -> genreRepository.findById(genre.id().longValue()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<MovieAliasEntity> mapAliases(java.util.List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return aliases.stream()
                .map(alias -> MovieAliasEntity.builder().alias(alias).build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<MovieVideoEntity> mapVideos(java.util.List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return videos.stream()
                .map(MovieMapper::mapVideo)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static MovieVideoEntity mapVideo(Video video) {
        MovieVideoEntity entity = MovieVideoEntity.builder()
                .resolution(video.getResolution())
                .url(video.getUrl())
                .size(video.getSize())
                .sizeInBytes(video.getSizeInBytes())
                .build();
        
        if (video.getCreatedAt() != null && !video.getCreatedAt().isEmpty()) {
            try {
                entity.setCreatedAt(java.time.LocalDateTime.parse(video.getCreatedAt(), FORMATTER));
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        return entity;
    }

    private static java.util.List<Genre> mapGenreDtos(Set<GenreEntity> genres) {
        if (genres == null || genres.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return genres.stream()
                .map(g -> new Genre(g.getId().intValue(), g.getName()))
                .collect(Collectors.toList());
    }

    private static java.util.List<String> mapAliasStrings(Set<MovieAliasEntity> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return aliases.stream()
                .map(MovieAliasEntity::getAlias)
                .collect(Collectors.toList());
    }

    private static java.util.List<Video> mapVideoDtos(Set<MovieVideoEntity> videos) {
        if (videos == null || videos.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return videos.stream()
                .map(MovieMapper::mapVideoDto)
                .collect(Collectors.toList());
    }

    private static Video mapVideoDto(MovieVideoEntity entity) {
        return Video.builder()
                .resolution(entity.getResolution())
                .url(entity.getUrl())
                .size(entity.getSize())
                .sizeInBytes(entity.getSizeInBytes())
                .createdAt(entity.getCreatedAt() != null ? 
                    entity.getCreatedAt().format(FORMATTER) : null)
                .build();
    }
}

