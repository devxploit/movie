package com.moviesp.builder.services;

import com.moviesp.builder.dtos.Folder;
import com.moviesp.builder.dtos.Movie;
import com.moviesp.builder.dtos.TvShow;
import com.moviesp.builder.entities.FolderEntity;
import com.moviesp.builder.entities.MovieEntity;
import com.moviesp.builder.entities.TvShowEntity;
import com.moviesp.builder.mappers.FolderMapper;
import com.moviesp.builder.mappers.MovieMapper;
import com.moviesp.builder.mappers.TvShowMapper;
import com.moviesp.builder.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DatabaseService {

    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final FolderRepository folderRepository;
    private final GenreRepository genreRepository;

    public DatabaseService(MovieRepository movieRepository,
            TvShowRepository tvShowRepository,
            FolderRepository folderRepository,
            GenreRepository genreRepository) {
        this.movieRepository = movieRepository;
        this.tvShowRepository = tvShowRepository;
        this.folderRepository = folderRepository;
        this.genreRepository = genreRepository;
    }

    @Transactional
    public void saveMovie(Movie movie) {
        try {
            Long tmdbId = Long.parseLong(movie.getId());
            Optional<MovieEntity> existing = movieRepository.findByTmdbId(tmdbId);

            MovieEntity entity;
            if (existing.isPresent()) {
                entity = existing.get();
                // Update simple fields
                entity.setName(movie.getTitle());
                entity.setPosterPath(movie.getPosterPath());
                entity.setUserScore(movie.getUserScore() != null ? BigDecimal.valueOf(movie.getUserScore()) : null);
                if (movie.getDate() != null) {
                    entity.setDate(java.time.LocalDate.parse(movie.getDate(),
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                }

                // Merge Genres
                entity.setGenres(MovieMapper.toEntity(movie, genreRepository).getGenres());

                // Merge Videos (Append new ones)
                // We need to implement a merge strategy. checking by URL or resolution?
                // For simplicity, let's add unique videos based on URL
                MovieEntity startEntity = MovieMapper.toEntity(movie, genreRepository);

                if (startEntity.getVideos() != null) {
                    for (var newVideo : startEntity.getVideos()) {
                        boolean exists = entity.getVideos().stream()
                                .anyMatch(v -> v.getUrl().equals(newVideo.getUrl()));
                        if (!exists) {
                            newVideo.setMovie(entity);
                            entity.getVideos().add(newVideo);
                        }
                    }
                }

                // Merge Aliases
                if (startEntity.getAliases() != null) {
                    for (var newAlias : startEntity.getAliases()) {
                        boolean exists = entity.getAliases().stream()
                                .anyMatch(a -> a.getAlias().equals(newAlias.getAlias()));
                        if (!exists) {
                            newAlias.setMovie(entity);
                            entity.getAliases().add(newAlias);
                        }
                    }
                }

                movieRepository.save(entity);
                log.info("Movie updated successfully: {}", movie.getTitle());

            } else {
                entity = MovieMapper.toEntity(movie, genreRepository);
                movieRepository.save(entity);
                log.info("Movie saved successfully: {}", movie.getTitle());
            }

        } catch (Exception e) {
            log.error("Error saving movie: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void saveMovies(List<Movie> movies) {
        for (Movie movie : movies) {
            saveMovie(movie); // Use the smart save for each
        }
    }

    @Transactional
    public void saveTvShow(TvShow tvShow) {
        try {
            Long tmdbId = Long.parseLong(tvShow.getId());
            Optional<TvShowEntity> existing = tvShowRepository.findByTmdbId(tmdbId);

            TvShowEntity entity;
            if (existing.isPresent()) {
                entity = existing.get();
                entity.setName(tvShow.getName());
                entity.setPosterPath(tvShow.getPosterPath());
                entity.setUserScore(tvShow.getUserScore() != null ? BigDecimal.valueOf(tvShow.getUserScore()) : null);

                TvShowEntity newEntity = TvShowMapper.toEntity(tvShow, genreRepository);

                entity.setGenres(newEntity.getGenres());

                // Merge Seasons/Episodes?
                // Currently TvShow entity structure is simple in mapper?
                // Let's assume full replacement of basic fields + merge logic if complex
                // relations exist.
                // Assuming Seasons are cascaded. Merge at Season level then Episode level is
                // complex.
                // For now, let's assume we WANT to update/add seasons.

                // Detailed merge of Seasons
                if (newEntity.getSeasons() != null) {
                    for (var newSeason : newEntity.getSeasons()) {
                        var existingSeasonOpt = entity.getSeasons().stream()
                                .filter(s -> s.getSeasonNumber().equals(newSeason.getSeasonNumber()))
                                .findFirst();

                        if (existingSeasonOpt.isPresent()) {
                            var existingSeason = existingSeasonOpt.get();
                            // Merge Episodes
                            for (var newEpisode : newSeason.getEpisodes()) {
                                boolean epExists = existingSeason.getEpisodes().stream()
                                        .anyMatch(e -> e.getEpisodeNumber().equals(newEpisode.getEpisodeNumber()));

                                if (!epExists) {
                                    newEpisode.setSeason(existingSeason);
                                    existingSeason.getEpisodes().add(newEpisode);
                                } else {
                                    // Update existing episode videos?
                                    // This gets deep. Let's assume add-missing-episodes strategy.
                                    var existingEp = existingSeason.getEpisodes().stream()
                                            .filter(e -> e.getEpisodeNumber().equals(newEpisode.getEpisodeNumber()))
                                            .findFirst().get();

                                    for (var video : newEpisode.getVideos()) {
                                        boolean vidExists = existingEp.getVideos().stream()
                                                .anyMatch(v -> v.getUrl().equals(video.getUrl()));
                                        if (!vidExists) {
                                            video.setEpisode(existingEp);
                                            existingEp.getVideos().add(video);
                                        }
                                    }
                                }
                            }
                        } else {
                            newSeason.setTvShow(entity);
                            entity.getSeasons().add(newSeason);
                        }
                    }
                }

                tvShowRepository.save(entity);
                log.info("TV Show updated successfully: {}", tvShow.getName());

            } else {
                entity = TvShowMapper.toEntity(tvShow, genreRepository);
                tvShowRepository.save(entity);
                log.info("TV Show saved successfully: {}", tvShow.getName());
            }

        } catch (Exception e) {
            log.error("Error saving TV show: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void saveTvShows(List<TvShow> tvShows) {
        for (TvShow tvShow : tvShows) {
            saveTvShow(tvShow);
        }
    }

    public List<Folder> getFolders() {
        try {
            List<FolderEntity> entities = folderRepository.findAll();
            return entities.stream()
                    .map(FolderMapper::toDto)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting folders: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void updateFolder(String path, String status, String user) {
        try {
            Optional<FolderEntity> existing = folderRepository.findByPathAndUser(path, user);

            if (existing.isPresent()) {
                FolderEntity entity = existing.get();
                entity.setStatus(status);
                folderRepository.save(entity);
                log.info("Folder updated: {} with status: {}", path, status);
            } else {
                FolderEntity entity = FolderEntity.builder()
                        .path(path)
                        .status(status)
                        .user(user)
                        .build();
                folderRepository.save(entity);
                log.info("Folder created: {} with status: {}", path, status);
            }
        } catch (Exception e) {
            log.error("Error updating folder: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean folderExists(String path, String user, String status) {
        return folderRepository.existsByPathAndUser(path, user);
    }
}
