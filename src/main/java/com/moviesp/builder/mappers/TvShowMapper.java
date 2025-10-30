package com.moviesp.builder.mappers;

import com.moviesp.builder.dtos.Episode;
import com.moviesp.builder.dtos.Genre;
import com.moviesp.builder.dtos.Season;
import com.moviesp.builder.dtos.TvShow;
import com.moviesp.builder.dtos.Video;
import com.moviesp.builder.entities.*;
import com.moviesp.builder.repositories.GenreRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TvShowMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static TvShowEntity toEntity(TvShow tvShow, GenreRepository genreRepository) {
        TvShowEntity entity = TvShowEntity.builder()
                .name(tvShow.getName())
                .date(tvShow.getDate() != null ? LocalDate.parse(tvShow.getDate(), DATE_FORMATTER) : null)
                .tmdbId(tvShow.getId() != null ? Long.parseLong(tvShow.getId()) : null)
                .posterPath(tvShow.getPosterPath())
                .userScore(tvShow.getUserScore() != null ? BigDecimal.valueOf(tvShow.getUserScore()) : null)
                .genres(mapGenres(tvShow.getGenres(), genreRepository))
                .aliases(mapAliases(tvShow.getAliases()))
                .seasons(mapSeasons(tvShow.getSeasons()))
                .build();

        // Set back references
        if (entity.getAliases() != null) {
            entity.getAliases().forEach(alias -> alias.setTvShow(entity));
        }
        if (entity.getSeasons() != null) {
            entity.getSeasons().forEach(season -> {
                season.setTvShow(entity);
                if (season.getEpisodes() != null) {
                    season.getEpisodes().forEach(episode -> {
                        episode.setSeason(season);
                        if (episode.getVideos() != null) {
                            episode.getVideos().forEach(video -> video.setEpisode(episode));
                        }
                    });
                }
            });
        }

        return entity;
    }

    public static TvShow toDto(TvShowEntity entity) {
        return TvShow.builder()
                .id(entity.getTmdbId() != null ? entity.getTmdbId().toString() : null)
                .date(entity.getDate() != null ? entity.getDate().format(DATE_FORMATTER) : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FORMATTER) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(FORMATTER) : null)
                .genres(mapGenreDtos(entity.getGenres()))
                .name(entity.getName())
                .aliases(mapAliasStrings(entity.getAliases()))
                .seasons(mapSeasonDtos(entity.getSeasons()))
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

    private static Set<TvShowAliasEntity> mapAliases(java.util.List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return aliases.stream()
                .map(alias -> TvShowAliasEntity.builder().alias(alias).build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<SeasonEntity> mapSeasons(java.util.List<Season> seasons) {
        if (seasons == null || seasons.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return seasons.stream()
                .map(TvShowMapper::mapSeason)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static SeasonEntity mapSeason(Season season) {
        SeasonEntity entity = SeasonEntity.builder()
                .name(season.getName())
                .seasonNumber(extractSeasonNumber(season.getName()))
                .userScore(season.getUserScore() != null ? BigDecimal.valueOf(season.getUserScore()) : null)
                .episodes(mapEpisodes(season.getEpisodes()))
                .build();

        // Parse date
        if (season.getDate() != null && !season.getDate().isEmpty()) {
            try {
                entity.setDate(LocalDate.parse(season.getDate(), DATE_FORMATTER));
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Parse createdAt
        if (season.getCreatedAt() != null && !season.getCreatedAt().isEmpty()) {
            try {
                entity.setCreatedAt(java.time.LocalDateTime.parse(season.getCreatedAt(), FORMATTER));
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        if (entity.getEpisodes() != null) {
            entity.getEpisodes().forEach(episode -> episode.setSeason(entity));
        }

        return entity;
    }

    private static Set<EpisodeEntity> mapEpisodes(java.util.List<Episode> episodes) {
        if (episodes == null || episodes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return episodes.stream()
                .map(TvShowMapper::mapEpisode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static EpisodeEntity mapEpisode(Episode episode) {
        EpisodeEntity entity = EpisodeEntity.builder()
                .episodeNumber(episode.getEpisodeNumber())
                .name(episode.getName())
                .videos(mapEpisodeVideos(episode.getVideos()))
                .build();

        if (entity.getVideos() != null) {
            entity.getVideos().forEach(video -> video.setEpisode(entity));
        }

        return entity;
    }

    private static Set<EpisodeVideoEntity> mapEpisodeVideos(java.util.List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return videos.stream()
                .map(TvShowMapper::mapEpisodeVideo)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static EpisodeVideoEntity mapEpisodeVideo(Video video) {
        EpisodeVideoEntity entity = EpisodeVideoEntity.builder()
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

    private static int extractSeasonNumber(String seasonName) {
        // Extract season number from season name (e.g., "Season 1" -> 1)
        if (seasonName != null && seasonName.matches(".*[sS]eason\\s*(\\d+).*")) {
            String[] parts = seasonName.split("[sS]eason\\s*");
            if (parts.length > 1) {
                try {
                    return Integer.parseInt(parts[1].trim().split("\\s")[0]);
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        }
        return 1;
    }

    private static java.util.List<Genre> mapGenreDtos(Set<GenreEntity> genres) {
        if (genres == null || genres.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return genres.stream()
                .map(g -> new Genre(g.getId().intValue(), g.getName()))
                .collect(Collectors.toList());
    }

    private static java.util.List<String> mapAliasStrings(Set<TvShowAliasEntity> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return aliases.stream()
                .map(TvShowAliasEntity::getAlias)
                .collect(Collectors.toList());
    }

    private static java.util.List<Season> mapSeasonDtos(Set<SeasonEntity> seasons) {
        if (seasons == null || seasons.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return seasons.stream()
                .map(TvShowMapper::mapSeasonDto)
                .collect(Collectors.toList());
    }

    private static Season mapSeasonDto(SeasonEntity entity) {
        return Season.builder()
                .name(entity.getName())
                .date(entity.getDate() != null ? entity.getDate().format(DATE_FORMATTER) : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FORMATTER) : null)
                .userScore(entity.getUserScore() != null ? entity.getUserScore().doubleValue() : null)
                .episodes(mapEpisodeDtos(entity.getEpisodes()))
                .build();
    }

    private static java.util.List<Episode> mapEpisodeDtos(Set<EpisodeEntity> episodes) {
        if (episodes == null || episodes.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return episodes.stream()
                .map(TvShowMapper::mapEpisodeDto)
                .collect(Collectors.toList());
    }

    private static Episode mapEpisodeDto(EpisodeEntity entity) {
        return Episode.builder()
                .episodeNumber(entity.getEpisodeNumber())
                .name(entity.getName())
                .season(entity.getSeason() != null ? entity.getSeason().getName() : null)
                .videos(mapEpisodeVideoDtos(entity.getVideos()))
                .build();
    }

    private static java.util.List<Video> mapEpisodeVideoDtos(Set<EpisodeVideoEntity> videos) {
        if (videos == null || videos.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return videos.stream()
                .map(TvShowMapper::mapEpisodeVideoDto)
                .collect(Collectors.toList());
    }

    private static Video mapEpisodeVideoDto(EpisodeVideoEntity entity) {
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

