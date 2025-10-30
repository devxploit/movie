package com.moviesp.builder.config;

import com.moviesp.builder.services.GenreSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GenreSyncRunner implements CommandLineRunner {

    private final GenreSyncService genreSyncService;

    public GenreSyncRunner(GenreSyncService genreSyncService) {
        this.genreSyncService = genreSyncService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting genre synchronization on application startup...");
        try {
            genreSyncService.syncGenresFromTMDB();
            log.info("Genre synchronization completed successfully on startup");
        } catch (Exception e) {
            log.error("Failed to synchronize genres on startup: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
        }
    }
}

