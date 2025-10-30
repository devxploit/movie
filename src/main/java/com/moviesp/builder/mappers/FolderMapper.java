package com.moviesp.builder.mappers;

import com.moviesp.builder.dtos.Folder;
import com.moviesp.builder.entities.FolderEntity;

import java.time.format.DateTimeFormatter;

public class FolderMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static FolderEntity toEntity(Folder folder) {
        FolderEntity entity = FolderEntity.builder()
                .path(folder.path())
                .user(folder.user())
                .status(folder.status())
                .build();
        
        // Only set timestamps if provided
        if (folder.createdAt() != null && !folder.createdAt().isEmpty()) {
            try {
                entity.setCreatedAt(java.time.LocalDateTime.parse(folder.createdAt(), FORMATTER));
            } catch (Exception e) {
                // Ignore parsing errors, use default
            }
        }
        
        if (folder.updatedAt() != null && !folder.updatedAt().isEmpty()) {
            try {
                entity.setUpdatedAt(java.time.LocalDateTime.parse(folder.updatedAt(), FORMATTER));
            } catch (Exception e) {
                // Ignore parsing errors, use default
            }
        }
        
        return entity;
    }

    public static Folder toDto(FolderEntity entity) {
        return new Folder(
                entity.getPath(),
                entity.getStatus(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().format(FORMATTER) : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(FORMATTER) : null,
                entity.getUser()
        );
    }
}

