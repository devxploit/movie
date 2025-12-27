package com.moviesp.builder.services;

import com.moviesp.builder.clients.CloudflareWorkerApiClient;
import com.moviesp.builder.dtos.*;
import com.moviesp.builder.dtos.cloudflare.FoldersResponse;
import com.moviesp.builder.usecases.*;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.json.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.moviesp.builder.config.Constants;

import java.util.*;

@Slf4j
@Service
public class IntegratorService {

    private final CloudflareWorkerApiClient cloudflareWorkerApiClient;
    private final DatabaseService databaseService;

    public IntegratorService(CloudflareWorkerApiClient cloudflareWorkerApiClient,
                            DatabaseService databaseService) {
        this.cloudflareWorkerApiClient = cloudflareWorkerApiClient;
        this.databaseService = databaseService;
    }

    public void firstData() {

        String mainPath = "/";
        final var resourceArgsUseCase = new ResourceArgsUseCase(mainPath);
        ResourcesArgs resourcesArgs = resourceArgsUseCase.generate();

        try {
            final var resourceUseCase = new ResourceUseCase(Constants.getLegacyUser(), Constants.getLegacyToken(), resourcesArgs);
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

                if(folders != null && folders.stream().anyMatch(folder -> Objects.nonNull(folder.path()) && folder.path().equals(res.getName()) && folder.status().equals("imported"))){
                    continue;
                }

                if(--batchDirProcess < 0){
                    break;
                }

                if(res.getType().equals("dir") && res.getName().toLowerCase().contains("serie")){
                    Map<String, List<TvshowItemUrl>> processedUrls = processMainSeriesDir(res);
                    allTvUrl.putAll(processedUrls);

                    // Update folder in both PostgreSQL and Cloudflare (dual write)
                    databaseService.updateFolder(res.getName(), "imported", Constants.getDefaultUser());
                    cloudflareWorkerApiClient.updateFolder(res.getName(), "imported", Constants.getDefaultUser()).block();

                }else if(res.getType().equals("dir") && (res.getName().toLowerCase().contains("pelicula") || res.getName().toLowerCase().contains("pelucula")) ){
                    Map<String, List<MovieItemUrl>> processedUrls = processMoviesResources(res);
                    allMoviesUrl.putAll(processedUrls);
                    
                    // Update folder in both PostgreSQL and Cloudflare (dual write)
                    databaseService.updateFolder(res.getName(), "imported", Constants.getDefaultUser());
                    cloudflareWorkerApiClient.updateFolder(res.getName(), "imported", Constants.getDefaultUser()).block();
                    
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

                if(movie.getTitle() == null || movie.getTitle().isEmpty()){
                    log.warn("  → Skipping movie with empty title for ID: {}", movieId);
                    continue;
                }

                movieBatch.add(movie);
                log.debug("  → Added movie to batch (current batch size: {})", movieBatch.size());

                if (movieBatch.size() >= batchSize || entry.equals(allMoviesUrl.entrySet().stream().reduce((first, second) -> second).orElse(null))) {
                    movieBatchCount++;
                    log.info("  → Saving batch {} of {} movies (batch size: {})", movieBatchCount, movieBatch.size(), batchSize);
                    try {
                        // Save to PostgreSQL
                        log.info("  → Saving movies batch to PostgreSQL...");
                        databaseService.saveMovies(movieBatch);
                        
                        // Save to Cloudflare (dual write)
                        log.info("  → Saving movies batch to Cloudflare...");
                        cloudflareWorkerApiClient.createMovies(movieBatch).block();
                        
                        log.info("  → ✓ Batch of {} movies saved successfully in both PostgreSQL and Cloudflare", movieBatch.size());
                        movieBatch.clear();
                        log.debug("  → Waiting 1.5 seconds before next batch...");
                        Thread.sleep(1500);
                    } catch (WebClientResponseException e) {
                        log.error("  → ✗ HTTP Error creating movie batch: Status {}, Response body: {}",
                                e.getStatusCode(),
                                e.getResponseBodyAsString()
                        );
                    } catch (Exception e) {
                        log.error("  → ✗ Unexpected error creating movie batch: {}", e.getMessage(), e);
                    }
                }
            }
            
            log.info("Step 10: Movie processing complete - Total movies processed: {}, Batches saved: {}", movieCount, movieBatchCount);

            log.info("Step 11: Processing TV shows in batches");
            List<TvShow> tvShowBatch = new ArrayList<>();
            int tvShowCount = 0;
            int tvShowBatchCount = 0;

            for (Map.Entry<String, List<TvshowItemUrl>> entry : allTvUrl.entrySet()) {
                String tvShowId = entry.getKey();
                List<TvshowItemUrl> tvshowItemUrls = entry.getValue();
                tvShowCount++;

                log.info("  → Building TV show {}/{} - ID: {}, URLs: {}", tvShowCount, allTvUrl.size(), tvShowId, tvshowItemUrls.size());
                TvShow tvShow = new TvShowUseCase().build(tvShowId, tvshowItemUrls);
                log.info("  → TV show built - ID: {}, Name: {}, Seasons: {}", tvShow.getId(), tvShow.getName(), tvShow.getSeasons().size());
                
                tvShowBatch.add(tvShow);
                log.debug("  → Added TV show to batch (current batch size: {})", tvShowBatch.size());

                if (tvShowBatch.size() >= batchSize || entry.equals(allTvUrl.entrySet().stream().reduce((first, second) -> second).orElse(null))) {
                    tvShowBatchCount++;
                    log.info("  → Saving batch {} of {} TV shows (batch size: {})", tvShowBatchCount, tvShowBatch.size(), batchSize);
                    try {
                        // Save to PostgreSQL
                        log.info("  → Saving TV shows batch to PostgreSQL...");
                        databaseService.saveTvShows(tvShowBatch);
                        
                        // Save to Cloudflare (dual write)
                        log.info("  → Saving TV shows batch to Cloudflare...");
                        cloudflareWorkerApiClient.createTvshows(tvShowBatch).block();
                        
                        log.info("  → ✓ Batch of {} TV shows saved successfully in both PostgreSQL and Cloudflare", tvShowBatch.size());
                        tvShowBatch.clear();
                        log.debug("  → Waiting 1.5 seconds before next batch...");
                        Thread.sleep(1500);
                    } catch (WebClientResponseException e) {
                        log.error("  → ✗ HTTP Error creating TV show batch: Status {}, Response body: {}",
                                e.getStatusCode(),
                                e.getResponseBodyAsString()
                        );
                    } catch (Exception e) {
                        log.error("  → ✗ Unexpected error creating TV show batch: {}", e.getMessage(), e);
                    }
                }
            }
            
            log.info("Step 12: TV show processing complete - Total TV shows processed: {}, Batches saved: {}", tvShowCount, tvShowBatchCount);


        } catch (Exception e) {
            log.error("========== ERROR in firstData() PROCESS ==========");
            log.error("Failed to connect to the Yandex Disk service: {}", e.getMessage(), e);
            log.error("========== ENDING firstData() PROCESS (ERROR) ==========");
            return;
        }

        log.info("========== SUCCESSFULLY COMPLETED firstData() PROCESS ==========");
    }

    //Esta solución puede causar ids repetidos si hay varias peliculas en diferentes directorios
    Map<String, List<MovieItemUrl>> processMoviesResources(Resource resource){
        log.info("  → [processMoviesResources] Starting processing for directory: {}", resource.getName());
        log.info("  → [processMoviesResources] Path: {}", resource.getPath().getPath());

        Map<String, List<MovieItemUrl>> resultMap = new java.util.HashMap<>();

        final var resourcesArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
        ResourcesArgs resourcesArgs = resourcesArgsUseCase.generate();

        final var resourceUseCase = new ResourceUseCase(Constants.getDefaultUser(), Constants.getDefaultToken(), resourcesArgs);
        Resource resourceDir = resourceUseCase.execute();

        if (resourceDir == null || resourceDir.getResourceList() == null) {
            log.error("  → [processMoviesResources] Failed to fetch resources for directory: {}", resource.getName());
            return resultMap;
        }

        log.info("  → [processMoviesResources] Directory: {} - Found {} items", resourceDir.getName(), resourceDir.getResourceList().getItems().size());

        List<Resource> resourceList = resourceDir.getResourceList().getItems();
        int processedItems = 0;
        int skippedItems = 0;

        for (Resource res : resourceList) {
            log.info("  → [processMoviesResources] Processing sub-resource: {} (Type: {})", res.getName(), res.getType());

            UrlUseCase urlUseCase = new UrlUseCase(res);
            MovieItemUrl itemUrl = urlUseCase.executeMovieUrl();

            if(itemUrl != null){
                List<MovieItemUrl> items = resultMap.containsKey(itemUrl.getId()) ? new ArrayList<>(resultMap.get(itemUrl.getId())): new ArrayList<>();
                if(items.isEmpty()){
                    resultMap.put(itemUrl.getId(), List.of(itemUrl));
                    log.debug("  → [processMoviesResources] Added new movie entry - ID: {}, Name: {}", itemUrl.getId(), itemUrl.getName());
                } else {
                    items.add(itemUrl);
                    resultMap.put(itemUrl.getId(), items);
                    log.debug("  → [processMoviesResources] Added additional URL to existing movie - ID: {}, Total URLs: {}", itemUrl.getId(), items.size());
                }
                processedItems++;
            } else {
                skippedItems++;
                log.debug("  → [processMoviesResources] Skipped sub-resource (null URL): {}", res.getName());
            }

        }

        log.info("  → [processMoviesResources] Completed - Processed: {}, Skipped: {}, Total movies: {}", processedItems, skippedItems, resultMap.size());
        return resultMap;

    }

    Map<String, List<TvshowItemUrl>> processMainSeriesDir(Resource resource){
        log.info("  → [processMainSeriesDir] Starting processing for series directory: {}", resource.getName());
        log.info("  → [processMainSeriesDir] Path: {}", resource.getPath().getPath());

        final var resourcesArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
        ResourcesArgs resourcesArgs = resourcesArgsUseCase.generate();

        final var resourceUseCase = new ResourceUseCase(Constants.getDefaultUser(), Constants.getDefaultToken(), resourcesArgs);
        Resource resourceDir = resourceUseCase.execute();

        if (resourceDir == null || resourceDir.getResourceList() == null) {
            log.error("  → [processMainSeriesDir] Failed to fetch resources for series directory: {}", resource.getName());
            return new java.util.HashMap<>();
        }

        log.info("  → [processMainSeriesDir] Series Directory: {} - Found {} TV shows", resourceDir.getName(), resourceDir.getResourceList().getItems().size());

        List<Resource> resourceList = resourceDir.getResourceList().getItems();

        Map<String, List<TvshowItemUrl>> resultMap = new java.util.HashMap<>();
        int processedEpisodes = 0;
        int skippedEpisodes = 0;

        for (Resource res : resourceList) {
            log.info("  → [processMainSeriesDir] Processing TV show: {}", res.getName());

            final var tvShowResourcesArgsUseCase = new ResourceArgsUseCase(res.getPath().getPath());
            ResourcesArgs seasonResourcesArgs = tvShowResourcesArgsUseCase.generate();

            final var tvShowResourceUseCase = new ResourceUseCase(Constants.getDefaultUser(), Constants.getDefaultToken(), seasonResourcesArgs);
            Resource seasonResourceDir = tvShowResourceUseCase.execute();

            if (seasonResourceDir == null || seasonResourceDir.getResourceList() == null) {
                log.warn("  → [processMainSeriesDir] Failed to fetch episodes for TV show: {}", res.getName());
                continue;
            }

            List<Resource> seasonResourceList = seasonResourceDir.getResourceList().getItems();
            log.debug("  → [processMainSeriesDir] TV show {} has {} episodes", res.getName(), seasonResourceList.size());

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

                if(itemUrl != null){
                    List<TvshowItemUrl> items = resultMap.containsKey(itemUrl.getId()) ? new ArrayList<>(resultMap.get(itemUrl.getId())): new ArrayList<>();
                    if(items.isEmpty()){
                        resultMap.put(itemUrl.getId(), List.of(itemUrl));
                        log.debug("  → [processMainSeriesDir] Added new TV show entry - ID: {}, Season: {}, Episode: {}", 
                                itemUrl.getId(), itemUrl.getSeason(), itemUrl.getEpisode());
                    } else {
                        items.add(itemUrl);
                        resultMap.put(itemUrl.getId(), items);
                        log.debug("  → [processMainSeriesDir] Added additional URL to existing TV show - ID: {}, Total URLs: {}", 
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


}
