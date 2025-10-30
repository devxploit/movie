package com.moviesp.builder.repositories;

import com.moviesp.builder.entities.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {
    
    Optional<FolderEntity> findByPathAndUser(String path, String user);
    
    boolean existsByPathAndUser(String path, String user);
    
    List<FolderEntity> findByUser(String user);
    
    List<FolderEntity> findByStatus(String status);
}

