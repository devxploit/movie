package com.moviesp.builder.repositories;

import com.moviesp.builder.entities.TvShowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TvShowRepository extends JpaRepository<TvShowEntity, Long> {
    
    Optional<TvShowEntity> findByTmdbId(Long tmdbId);
    
    boolean existsByTmdbId(Long tmdbId);
}

