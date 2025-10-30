package com.moviesp.builder.repositories;

import com.moviesp.builder.entities.GenreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<GenreEntity, Long> {
    
    Optional<GenreEntity> findByIdAndType(Long id, String type);
    
    List<GenreEntity> findByType(String type);
    
    boolean existsByIdAndType(Long id, String type);
}

