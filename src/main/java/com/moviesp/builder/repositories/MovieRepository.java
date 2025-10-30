package com.moviesp.builder.repositories;

import com.moviesp.builder.entities.MovieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<MovieEntity, Long> {
    
    Optional<MovieEntity> findByTmdbId(Long tmdbId);
    
    boolean existsByTmdbId(Long tmdbId);
}

