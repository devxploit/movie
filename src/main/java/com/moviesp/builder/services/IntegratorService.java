package com.moviesp.builder.services;

import com.moviesp.builder.clients.CloudflareWorkerApiClient;
import com.moviesp.builder.dtos.*;
import com.moviesp.builder.usecases.*;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.json.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.moviesp.builder.config.Constants;

import java.util.*;

@Slf4j
@Service
public class IntegratorService {

    private final CloudflareWorkerApiClient cloudflareWorkerApiClient;
    private final DatabaseService databaseService;
    private final Constants constants;

    public IntegratorService(CloudflareWorkerApiClient cloudflareWorkerApiClient,
            DatabaseService databaseService, Constants constants) {
        this.cloudflareWorkerApiClient = cloudflareWorkerApiClient;
        this.databaseService = databaseService;
        this.constants = constants;
    }

    private String getUser(String generator) {
        if ("updates".equalsIgnoreCase(generator)) {
            return constants.getUpdatesUser();
        }
        return Constants.getLegacyUser();
    }

    private String getToken(String generator) {
        if ("updates".equalsIgnoreCase(generator)) {
            return constants.getUpdatesToken();
        }
        return Constants.getLegacyToken();
    }

    public void firstData(String generator) {

        String mainPath = "/";
        final var resourceArgsUseCase = new ResourceArgsUseCase(mainPath);
        ResourcesArgs resourcesArgs = resourceArgsUseCase.generate();

        try {
            final var resourceUseCase = new ResourceUseCase(getUser(generator), getToken(generator),
                    resourcesArgs);
            Resource resource = resourceUseCase.execute();

            if (resource == null || resource.getResourceList() == null) {
                log.error("Failed to fetch resources or resource list is null");
                return;
            }

            List<Resource> resourceList = resource.getResourceList().getItems();

            if (resourceList.isEmpty()) {
                log.warn("No resources found in the specified path: {}", mainPath);
                return;
            }

            int batchDirProcess = 5;

            // Get folders from PostgreSQL database
            List<Folder> folders = databaseService.getFolders();

            Map<String, List<MovieItemUrl>> allMoviesUrl = new java.util.HashMap<>();
            Map<String, List<TvshowItemUrl>> allTvUrl = new java.util.HashMap<>();

            for (Resource res : resourceList) {

                if (folders != null && folders.stream().anyMatch(folder -> Objects.nonNull(folder.path())
                        && folder.path().equals(res.getName()) && folder.status().equals("imported"))) {
                    continue;
                }

                if (--batchDirProcess < 0) {
                    break;
                }

                if (res.getType().equals("dir") && res.getName().toLowerCase().contains("serie")) {
                    Map<String, List<TvshowItemUrl>> processedUrls = processMainSeriesDir(res, generator);
                    allTvUrl.putAll(processedUrls);

                    // Update folder in both PostgreSQL and Cloudflare (dual write)
                    databaseService.updateFolder(res.getName(), "imported", Constants.getDefaultUser());
                    // Cloudflare service commented out as requested
                    // cloudflareWorkerApiClient.updateFolder(res.getName(), "imported",
                    // Constants.getDefaultUser()).block();

                } else if (res.getType().equals("dir") && (res.getName().toLowerCase().contains("pelicula")
                        || res.getName().toLowerCase().contains("pelucula"))) {
                    Map<String, List<MovieItemUrl>> processedUrls = processMoviesResources(res, generator);
                    allMoviesUrl.putAll(processedUrls);

                    // Update folder in both PostgreSQL and Cloudflare (dual write)
                    databaseService.updateFolder(res.getName(), "imported", Constants.getDefaultUser());
                    // Cloudflare service commented out as requested
                    // cloudflareWorkerApiClient.updateFolder(res.getName(), "imported",
                    // Constants.getDefaultUser()).block();

                }

            }

            int batchSize = 20;
            List<Movie> movieBatch = new ArrayList<>();
            int movieCount = 0;
            int movieBatchCount = 0;

            for (Map.Entry<String, List<MovieItemUrl>> entry : allMoviesUrl.entrySet()) {
                String movieId = entry.getKey();
                List<MovieItemUrl> movieUrls = entry.getValue();
                movieCount++;

                Movie movie = new MovieUseCase().build(movieId, movieUrls);

                if (movie.getTitle() == null || movie.getTitle().isEmpty()) {
                    log.warn("  → Skipping movie with empty title for ID: {}", movieId);
                    continue;
                }

                movieBatch.add(movie);
                log.debug("  → Added movie to batch (current batch size: {})", movieBatch.size());

                if (movieBatch.size() >= batchSize || entry
                        .equals(allMoviesUrl.entrySet().stream().reduce((first, second) -> second).orElse(null))) {
                    movieBatchCount++;
                    log.info("  → Saving batch {} of {} movies (batch size: {})", movieBatchCount, movieBatch.size(),
                            batchSize);

                    // Save to PostgreSQL with fault tolerance
                    safeSaveMoviesBatch(movieBatch);

                    // Save to Cloudflare (dual write) - Commented out
                    // log.info(" → Saving movies batch to Cloudflare...");
                    // cloudflareWorkerApiClient.createMovies(movieBatch).block();

                    movieBatch.clear();
                    log.debug("  → Waiting 1.5 seconds before next batch...");
                    Thread.sleep(1500);
                }
            }

            log.info("Step 10: Movie processing complete - Total movies processed: {}, Batches saved: {}", movieCount,
                    movieBatchCount);

            log.info("Step 11: Processing TV shows in batches");
            List<TvShow> tvShowBatch = new ArrayList<>();
            int tvShowCount = 0;
            int tvShowBatchCount = 0;

            for (Map.Entry<String, List<TvshowItemUrl>> entry : allTvUrl.entrySet()) {
                String tvShowId = entry.getKey();
                List<TvshowItemUrl> tvshowItemUrls = entry.getValue();
                tvShowCount++;

                log.info("  → Building TV show {}/{} - ID: {}, URLs: {}", tvShowCount, allTvUrl.size(), tvShowId,
                        tvshowItemUrls.size());
                TvShow tvShow = new TvShowUseCase().build(tvShowId, tvshowItemUrls);
                log.info("  → TV show built - ID: {}, Name: {}, Seasons: {}", tvShow.getId(), tvShow.getName(),
                        tvShow.getSeasons().size());

                tvShowBatch.add(tvShow);
                log.debug("  → Added TV show to batch (current batch size: {})", tvShowBatch.size());

                if (tvShowBatch.size() >= batchSize
                        || entry.equals(allTvUrl.entrySet().stream().reduce((first, second) -> second).orElse(null))) {
                    tvShowBatchCount++;
                    log.info("  → Saving batch {} of {} TV shows (batch size: {})", tvShowBatchCount,
                            tvShowBatch.size(), batchSize);

                    // Save to PostgreSQL with fault tolerance
                    safeSaveTvShowsBatch(tvShowBatch);

                    // Save to Cloudflare (dual write) - Commented out
                    // log.info(" → Saving TV shows batch to Cloudflare...");
                    // cloudflareWorkerApiClient.createTvshows(tvShowBatch).block();

                    tvShowBatch.clear();
                    log.debug("  → Waiting 1.5 seconds before next batch...");
                    Thread.sleep(1500);
                }
            }

            log.info("Step 12: TV show processing complete - Total TV shows processed: {}, Batches saved: {}",
                    tvShowCount, tvShowBatchCount);

        } catch (Exception e) {
            log.error("========== ERROR in firstData() PROCESS ==========");
            log.error("Failed to connect to the Yandex Disk service: {}", e.getMessage(), e);
            log.error("========== ENDING firstData() PROCESS (ERROR) ==========");
            return;
        }

        log.info("========== SUCCESSFULLY COMPLETED firstData() PROCESS ==========");
    }

    // Esta solución puede causar ids repetidos si hay varias peliculas en
    // diferentes directorios
    Map<String, List<MovieItemUrl>> processMoviesResources(Resource resource, String generator) {
        log.info("  → [processMoviesResources] Starting processing for directory: {}", resource.getName());
        log.info("  → [processMoviesResources] Path: {}", resource.getPath().getPath());

        Map<String, List<MovieItemUrl>> resultMap = new java.util.HashMap<>();

        final var resourcesArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
        ResourcesArgs resourcesArgs = resourcesArgsUseCase.generate();

        final var resourceUseCase = new ResourceUseCase(getUser(generator), getToken(generator),
                resourcesArgs);
        Resource resourceDir = resourceUseCase.execute();

        if (resourceDir == null || resourceDir.getResourceList() == null) {
            log.error("  → [processMoviesResources] Failed to fetch resources for directory: {}", resource.getName());
            return resultMap;
        }

        log.info("  → [processMoviesResources] Directory: {} - Found {} items", resourceDir.getName(),
                resourceDir.getResourceList().getItems().size());

        List<Resource> resourceList = resourceDir.getResourceList().getItems();
        int processedItems = 0;
        int skippedItems = 0;

        for (Resource res : resourceList) {
            log.info("  → [processMoviesResources] Processing sub-resource: {} (Type: {})", res.getName(),
                    res.getType());

            UrlUseCase urlUseCase = new UrlUseCase(res);
            MovieItemUrl itemUrl = urlUseCase.executeMovieUrl();

            if (itemUrl != null) {
                List<MovieItemUrl> items = resultMap.containsKey(itemUrl.getId())
                        ? new ArrayList<>(resultMap.get(itemUrl.getId()))
                        : new ArrayList<>();
                if (items.isEmpty()) {
                    resultMap.put(itemUrl.getId(), List.of(itemUrl));
                    log.debug("  → [processMoviesResources] Added new movie entry - ID: {}, Name: {}", itemUrl.getId(),
                            itemUrl.getName());
                } else {
                    items.add(itemUrl);
                    resultMap.put(itemUrl.getId(), items);
                    log.debug(
                            "  → [processMoviesResources] Added additional URL to existing movie - ID: {}, Total URLs: {}",
                            itemUrl.getId(), items.size());
                }
                processedItems++;
            } else {
                skippedItems++;
                log.debug("  → [processMoviesResources] Skipped sub-resource (null URL): {}", res.getName());
            }

        }

        log.info("  → [processMoviesResources] Completed - Processed: {}, Skipped: {}, Total movies: {}",
                processedItems, skippedItems, resultMap.size());
        return resultMap;

    }

    Map<String, List<TvshowItemUrl>> processMainSeriesDir(Resource resource, String generator) {
        log.info("  → [processMainSeriesDir] Starting processing for series directory: {}", resource.getName());
        log.info("  → [processMainSeriesDir] Path: {}", resource.getPath().getPath());

        final var resourcesArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
        ResourcesArgs resourcesArgs = resourcesArgsUseCase.generate();

        final var resourceUseCase = new ResourceUseCase(getUser(generator), getToken(generator),
                resourcesArgs);
        Resource resourceDir = resourceUseCase.execute();

        if (resourceDir == null || resourceDir.getResourceList() == null) {
            log.error("  → [processMainSeriesDir] Failed to fetch resources for series directory: {}",
                    resource.getName());
            return new java.util.HashMap<>();
        }

        log.info("  → [processMainSeriesDir] Series Directory: {} - Found {} TV shows", resourceDir.getName(),
                resourceDir.getResourceList().getItems().size());

        List<Resource> resourceList = resourceDir.getResourceList().getItems();

        Map<String, List<TvshowItemUrl>> resultMap = new java.util.HashMap<>();
        int processedEpisodes = 0;
        int skippedEpisodes = 0;

        for (Resource res : resourceList) {
            log.info("  → [processMainSeriesDir] Processing TV show: {}", res.getName());

            final var tvShowResourcesArgsUseCase = new ResourceArgsUseCase(res.getPath().getPath());
            ResourcesArgs seasonResourcesArgs = tvShowResourcesArgsUseCase.generate();

            final var tvShowResourceUseCase = new ResourceUseCase(getUser(generator),
                    getToken(generator), seasonResourcesArgs);
            Resource seasonResourceDir = tvShowResourceUseCase.execute();

            if (seasonResourceDir == null || seasonResourceDir.getResourceList() == null) {
                log.warn("  → [processMainSeriesDir] Failed to fetch episodes for TV show: {}", res.getName());
                continue;
            }

            List<Resource> seasonResourceList = seasonResourceDir.getResourceList().getItems();
            log.debug("  → [processMainSeriesDir] TV show {} has {} episodes", res.getName(),
                    seasonResourceList.size());

            String[] nameSplit = res.getName().split("id_");
            if (nameSplit.length < 2) {
                log.error("  → [processMainSeriesDir] Invalid TV show name format (missing 'id_'): {}", res.getName());
                continue;
            }

            String id = nameSplit[1];
            String name = nameSplit[0].replaceAll("\\s*T_\\d+", "").trim();
            log.debug("  → [processMainSeriesDir] Extracted TV show - ID: {}, Name: {}", id, name);

            for (Resource seasonRes : seasonResourceList) {
                log.debug("  → [processMainSeriesDir] Processing episode: {}", seasonRes.getName());

                UrlUseCase urlUseCase = new UrlUseCase(seasonRes);
                TvshowItemUrl itemUrl = urlUseCase.executeTvShowUrl(name, id);

                if (itemUrl != null) {
                    List<TvshowItemUrl> items = resultMap.containsKey(itemUrl.getId())
                            ? new ArrayList<>(resultMap.get(itemUrl.getId()))
                            : new ArrayList<>();
                    if (items.isEmpty()) {
                        resultMap.put(itemUrl.getId(), List.of(itemUrl));
                        log.debug(
                                "  → [processMainSeriesDir] Added new TV show entry - ID: {}, Season: {}, Episode: {}",
                                itemUrl.getId(), itemUrl.getSeason(), itemUrl.getEpisode());
                    } else {
                        items.add(itemUrl);
                        resultMap.put(itemUrl.getId(), items);
                        log.debug(
                                "  → [processMainSeriesDir] Added additional URL to existing TV show - ID: {}, Total URLs: {}",
                                itemUrl.getId(), items.size());
                    }
                    processedEpisodes++;
                } else {
                    skippedEpisodes++;
                    log.debug("  → [processMainSeriesDir] Skipped episode (null URL): {}", seasonRes.getName());
                }

            }

        }

        log.info("  → [processMainSeriesDir] Completed - Processed episodes: {}, Skipped: {}, Total TV shows: {}",
                processedEpisodes, skippedEpisodes, resultMap.size());

        return resultMap;

    }

    private void safeSaveMoviesBatch(List<Movie> movieBatch) {
        if (movieBatch.isEmpty())
            return;

        try {
            log.info("  → Saving movies batch to PostgreSQL...");
            databaseService.saveMovies(movieBatch);
            log.info("  → ✓ Batch of {} movies saved successfully", movieBatch.size());
        } catch (Exception e) {
            log.error("  → ✗ Error saving movies batch. Attempting to save individually...", e);
            int savedCount = 0;
            int failedCount = 0;

            for (Movie movie : movieBatch) {
                try {
                    databaseService.saveMovie(movie);
                    savedCount++;
                } catch (Exception ex) {
                    failedCount++;
                    log.error("  → ✗ Failed to save individual movie: {} (ID: {}). Error: {}",
                            movie.getTitle(), movie.getId(), ex.getMessage());
                }
            }
            log.info("  → Individual save complete. Saved: {}, Failed: {}", savedCount, failedCount);
        }
    }

    private void safeSaveTvShowsBatch(List<TvShow> tvShowBatch) {
        if (tvShowBatch.isEmpty())
            return;

        try {
            log.info("  → Saving TV shows batch to PostgreSQL...");
            databaseService.saveTvShows(tvShowBatch);
            log.info("  → ✓ Batch of {} TV shows saved successfully", tvShowBatch.size());
        } catch (Exception e) {
            log.error("  → ✗ Error saving TV shows batch. Attempting to save individually...", e);
            int savedCount = 0;
            int failedCount = 0;

            for (TvShow tvShow : tvShowBatch) {
                try {
                    databaseService.saveTvShow(tvShow);
                    savedCount++;
                } catch (Exception ex) {
                    failedCount++;
                    log.error("  → ✗ Failed to save individual TV show: {} (ID: {}). Error: {}",
                            tvShow.getName(), tvShow.getId(), ex.getMessage());
                }
            }
            log.info("  → Individual save complete. Saved: {}, Failed: {}", savedCount, failedCount);
        }
    }

    public void importFolder(String path, String type, String generator) {
        log.info("Starting manual import for path: {} (Type: {}, Generator: {})", path, type, generator);

        final var resourceArgsUseCase = new ResourceArgsUseCase(path);
        ResourcesArgs resourcesArgs = resourceArgsUseCase.generate();

        try {
            final var resourceUseCase = new ResourceUseCase(getUser(generator), getToken(generator), resourcesArgs);
            Resource resource = resourceUseCase.execute();

            if (resource == null) {
                log.error("Failed to fetch resource for path: {}", path);
                return;
            }

            if ("movie".equalsIgnoreCase(type)) {
                log.info("Importing as Movie: {}", resource.getName());
                UrlUseCase urlUseCase = new UrlUseCase(resource);
                MovieItemUrl itemUrl = urlUseCase.executeMovieUrl();

                if (itemUrl != null) {
                    Movie movie = new MovieUseCase().build(itemUrl.getId(), Collections.singletonList(itemUrl));
                    if (movie.getTitle() != null && !movie.getTitle().isEmpty()) {
                        databaseService.saveMovie(movie);
                        log.info("Successfully imported movie: {}", movie.getTitle());
                    } else {
                        log.warn("Movie title empty, skipping save.");
                    }
                } else {
                    log.warn("Could not extract Movie URL/Info from path: {}", path);
                }

            } else if ("tvshow".equalsIgnoreCase(type)) {
                log.info("Importing as TV Show: {}", resource.getName());

                // For TV Show, we need to list the seasons/episodes inside
                final var tvShowResourceUseCase = new ResourceUseCase(getUser(generator), getToken(generator),
                        resourcesArgs);
                Resource seasonResourceDir = tvShowResourceUseCase.execute(); // Re-fetch? Already fetched above. Logic
                                                                              // checks out.

                if (seasonResourceDir != null && seasonResourceDir.getResourceList() != null) {
                    List<Resource> seasonResourceList = seasonResourceDir.getResourceList().getItems();

                    String[] nameSplit = resource.getName().split("id_");
                    if (nameSplit.length < 2) {
                        log.error("Invalid TV show name format (missing 'id_'): {}", resource.getName());
                        return;
                    }
                    String id = nameSplit[1];
                    String name = nameSplit[0].replaceAll("\\s*T_\\d+", "").trim();

                    List<TvshowItemUrl> tvshowItemUrls = new ArrayList<>();

                    for (Resource seasonRes : seasonResourceList) {
                        UrlUseCase urlUseCase = new UrlUseCase(seasonRes);
                        TvshowItemUrl itemUrl = urlUseCase.executeTvShowUrl(name, id);
                        if (itemUrl != null) {
                            tvshowItemUrls.add(itemUrl);
                        }
                    }

                    if (!tvshowItemUrls.isEmpty()) {
                        TvShow tvShow = new TvShowUseCase().build(id, tvshowItemUrls);
                        databaseService.saveTvShow(tvShow);
                        log.info("Successfully imported TV Show: {}", tvShow.getName());
                    } else {
                        log.warn("No valid episodes found for TV Show: {}", name);
                    }
                }

            } else {
                log.warn("Unknown type: {}", type);
            }

        } catch (Exception e) {
            log.error("Error importing folder: {}", e.getMessage(), e);
            throw new RuntimeException("Import failed", e);
        }
    }

    public void getNewUrls(String generator) {
        log.info("Starting 'Get New URLs' scan with generator: {}", generator);

        // Reusing scanning logic from firstData, but without clearing previous data
        // because safeSaveMoviesBatch handles upserts now.
        // Actually, firstData implementation already does the scan.
        // The difference is that firstData iterates EVERYTHING.
        // If "Get New URLs" assumes scanning the "Updates" folder structure which
        // mimics the main structure,
        // then reusing the logic is fine.

        // Let's call the internal scanning logic directly.
        // But firstData is "void" and does everything.
        // Let's verify if firstData logic is exactly what we need.
        // firstData:
        // 1. Get resources from "/"
        // 2. Iterate folders.
        // 3. Process Movies/Series.
        // 4. Batch save.

        // Since we updated safeSave* to use DatabaseService's smart upsert,
        // calling firstData(generator) is effectively "Get New URLs" if the generator
        // points to the update source.

        log.info("Delegating to firstData logic for scanning...");
        firstData(generator);

        log.info("'Get New URLs' scan completed.");
    }

}
