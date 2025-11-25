package com.moviesp.builder.services;

import com.moviesp.builder.dtos.Folder;
import com.moviesp.builder.dtos.Movie;
import com.moviesp.builder.dtos.TvShow;
import com.moviesp.builder.entities.FolderEntity;
import com.moviesp.builder.entities.GenreEntity;
import com.moviesp.builder.entities.MovieEntity;
import com.moviesp.builder.entities.TvShowEntity;
import com.moviesp.builder.mappers.FolderMapper;
import com.moviesp.builder.mappers.MovieMapper;
import com.moviesp.builder.mappers.TvShowMapper;
import com.moviesp.builder.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            MovieEntity entity = MovieMapper.toEntity(movie, genreRepository);
            movieRepository.save(entity);
            log.info("Movie saved successfully: {}", movie.getTitle());
        } catch (Exception e) {
            log.error("Error saving movie: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void saveMovies(List<Movie> movies) {
        try {
            List<MovieEntity> entities = movies.stream()
                    .map(movie -> MovieMapper.toEntity(movie, genreRepository))
                    .toList();
            
            movieRepository.saveAll(entities);
            log.info("Batch of {} movies saved successfully", movies.size());
        } catch (Exception e) {
            log.error("Error saving movies batch: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void saveTvShow(TvShow tvShow) {
        try {
            TvShowEntity entity = TvShowMapper.toEntity(tvShow, genreRepository);
            tvShowRepository.save(entity);
            log.info("TV Show saved successfully: {}", tvShow.getName());
        } catch (Exception e) {
            log.error("Error saving TV show: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void saveTvShows(List<TvShow> tvShows) {
        try {
            List<TvShowEntity> entities = tvShows.stream()
                    .map(tvShow -> TvShowMapper.toEntity(tvShow, genreRepository))
                    .toList();
            
            tvShowRepository.saveAll(entities);
            log.info("Batch of {} TV shows saved successfully", tvShows.size());
        } catch (Exception e) {
            log.error("Error saving TV shows batch: {}", e.getMessage(), e);
            throw e;
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

