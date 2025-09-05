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

import static com.moviesp.builder.config.Constants.*;

import java.util.*;

@Slf4j
@Service
public class IntegratorService {

    private final CloudflareWorkerApiClient cloudflareWorkerApiClient;

    public IntegratorService(CloudflareWorkerApiClient cloudflareWorkerApiClient) {
        this.cloudflareWorkerApiClient = cloudflareWorkerApiClient;
    }

    public void firstData() {

        String mainPath = "/";
        final var resourceArgsUseCase = new ResourceArgsUseCase(mainPath);
        ResourcesArgs resourcesArgs = resourceArgsUseCase.generate();

        try {
            final var resourceUseCase = new ResourceUseCase(DEFAULT_USER, DEFAULT_TOKEN, resourcesArgs);
            Resource resource = resourceUseCase.execute();
            List<Resource> resourceList =  resource.getResourceList().getItems();

            if (resourceList.isEmpty()) {
                log.info("No resources found in the specified path: {}", mainPath);
                return;
            }

            int batchDirProcess = 5;

            FoldersResponse foldersResponse = cloudflareWorkerApiClient.getFolders().block();
            List<Folder> folders = foldersResponse != null && foldersResponse.success() ? foldersResponse.results() : null;

            Map<String, List<MovieItemUrl>> allMoviesUrl = new java.util.HashMap<>();
            Map<String, List<TvshowItemUrl>> allTvUrl = new java.util.HashMap<>();
            for (Resource res : resourceList) {

                if(folders != null && folders.stream().anyMatch(folder -> Objects.nonNull(folder.path()) && folder.path().equals(res.getName()) && folder.status().equals("imported"))){
                    log.info(" - Skipping already imported resource: {}", res.getName());
                    continue;
                }

                if(--batchDirProcess < 0){
                    log.info(" - Batch directory process limit reached, stopping further processing.");
                    break;
                }

                log.info("Resource: {} , Type {}: ", res.getName(), res.getType());
                if(res.getType().equals("dir") && res.getName().toLowerCase().contains("serie")){

                    Map<String, List<TvshowItemUrl>> processedUrls = processMainSeriesDir(res);
                    allTvUrl.putAll(processedUrls);

                    cloudflareWorkerApiClient.updateFolder(res.getName(), "imported", DEFAULT_USER).block();

                }else if(res.getType().equals("dir") && (res.getName().toLowerCase().contains("pelicula") || res.getName().toLowerCase().contains("pelucula")) ){

                    Map<String, List<MovieItemUrl>> processedUrls = processMoviesResources(res);
                    allMoviesUrl.putAll(processedUrls);
                    cloudflareWorkerApiClient.updateFolder(res.getName(), "imported", DEFAULT_USER).block();
                } else {
                    log.info(" - Skipping non-directory resource: {}", res.getName());
                }

            }

            //log.info("allMoviesUrl: {}", allMoviesUrl);
            //log.info("allTvUrl: {}", allTvUrl);

            int batchSize = 20;
            List<Movie> movieBatch = new ArrayList<>();

            for (Map.Entry<String, List<MovieItemUrl>> entry : allMoviesUrl.entrySet()) {
                String movieId = entry.getKey();
                List<MovieItemUrl> movieUrls = entry.getValue();

                Movie movie = new MovieUseCase().build(movieId, movieUrls);
                log.info("Processing movie with ID: {} and Title: {}", movie.getId(), movie.getTitle());

                if(movie.getTitle() == null || movie.getTitle().isEmpty()){
                    log.warn(" - Skipping movie with empty title for ID: {}", movieId);
                    continue;
                }

                movieBatch.add(movie);

                if (movieBatch.size() >= batchSize || entry.equals(allMoviesUrl.entrySet().stream().reduce((first, second) -> second).orElse(null))) {
                    try {
                        cloudflareWorkerApiClient.createMovies(movieBatch).block();
                        log.info("Batch of {} movies created successfully", movieBatch.size());
                        movieBatch.clear();
                        Thread.sleep(1500);
                    } catch (WebClientResponseException e) {
                        log.error("HTTP Error creating movie batch: Status {}, Response body: {}",
                                e.getStatusCode(),
                                e.getResponseBodyAsString()
                        );
                    } catch (Exception e) {
                        log.error("Unexpected error creating movie batch: {}", e.getMessage(), e);
                    }
                }
            }

            List<TvShow> tvShowBatch = new ArrayList<>();

            for (Map.Entry<String, List<TvshowItemUrl>> entry : allTvUrl.entrySet()) {

                String tvShowId = entry.getKey();
                List<TvshowItemUrl> tvshowItemUrls = entry.getValue();
                TvShow tvShow = new TvShowUseCase().build(tvShowId, tvshowItemUrls);
                tvShowBatch.add(tvShow);

                if (tvShowBatch.size() >= batchSize || entry.equals(allTvUrl.entrySet().stream().reduce((first, second) -> second).orElse(null))) {
                    try {
                        cloudflareWorkerApiClient.createTvshows(tvShowBatch).block();
                        log.info("Batch of {} TV show episodes created successfully", tvShowBatch.size());
                        tvShowBatch.clear();
                        Thread.sleep(1500);
                    } catch (WebClientResponseException e) {
                        log.error("HTTP Error creating TV show batch: Status {}, Response body: {}",
                                e.getStatusCode(),
                                e.getResponseBodyAsString()
                        );
                    } catch (Exception e) {
                        log.error("Unexpected error creating TV show batch: {}", e.getMessage(), e);
                    }
                }
            }


        } catch (Exception e) {
            log.error("Failed to connect to the Yandex Disk service: {}", e.getMessage());
            return;
        }

    }

    //Esta solución puede causar ids repetidos si hay varias peliculas en diferentes directorios
    Map<String, List<MovieItemUrl>> processMoviesResources(Resource resource){

        Map<String, List<MovieItemUrl>> resultMap = new java.util.HashMap<>();

        final var resourcesArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
        ResourcesArgs resourcesArgs = resourcesArgsUseCase.generate();

        final var resourceUseCase = new ResourceUseCase(DEFAULT_USER, DEFAULT_TOKEN, resourcesArgs);
        Resource resourceDir = resourceUseCase.execute();

        log.warn("Directory: {} ", resourceDir.getName());

        List<Resource> resourceList =  resourceDir.getResourceList().getItems();

        for (Resource res : resourceList) {
            log.info(" - Sub Resource: {}", res.getName());

            UrlUseCase urlUseCase = new UrlUseCase(res);
            MovieItemUrl itemUrl = urlUseCase.executeMovieUrl();

            if(itemUrl != null){
                List<MovieItemUrl> items = resultMap.containsKey(itemUrl.getId()) ? new ArrayList<>(resultMap.get(itemUrl.getId())): new ArrayList<>();
                if(items.isEmpty()){
                    resultMap.put(itemUrl.getId(), List.of(itemUrl));
                } else {
                    items.add(itemUrl);
                    resultMap.put(itemUrl.getId(), items);
                }
            }

        }

        return  resultMap;

    }

    Map<String, List<TvshowItemUrl>> processMainSeriesDir(Resource resource){

        final var resourcesArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
        ResourcesArgs resourcesArgs = resourcesArgsUseCase.generate();

        final var resourceUseCase = new ResourceUseCase(DEFAULT_USER, DEFAULT_TOKEN, resourcesArgs);
        Resource resourceDir = resourceUseCase.execute();

        log.warn(" Series Directory: {} ", resourceDir.getName());

        List<Resource> resourceList =  resourceDir.getResourceList().getItems();

        Map<String, List<TvshowItemUrl>> resultMap = new java.util.HashMap<>();

        for (Resource res : resourceList) {
            //log.info(" -- TvShow Resource: {}", res.getName());

            final var tvShowResourcesArgsUseCase = new ResourceArgsUseCase(res.getPath().getPath());
            ResourcesArgs seasonResourcesArgs = tvShowResourcesArgsUseCase.generate();

            final var tvShowResourceUseCase = new ResourceUseCase(DEFAULT_USER, DEFAULT_TOKEN, seasonResourcesArgs);
            Resource seasonResourceDir = tvShowResourceUseCase.execute();

            List<Resource> seasonResourceList =  seasonResourceDir.getResourceList().getItems();

            String[] nameSplit = res.getName().split("id_");
            String id = nameSplit[1];
            String name = nameSplit[0].replaceAll("\\s*T_\\d+", "").trim();

            for (Resource seasonRes : seasonResourceList) {

                //log.info("   - Episode Resource: {}", seasonRes.getName());

                UrlUseCase urlUseCase = new UrlUseCase(seasonRes);
                TvshowItemUrl itemUrl = urlUseCase.executeTvShowUrl(name, id);

                if(itemUrl != null){
                    List<TvshowItemUrl> items = resultMap.containsKey(itemUrl.getId()) ? new ArrayList<>(resultMap.get(itemUrl.getId())): new ArrayList<>();
                    if(items.isEmpty()){
                        resultMap.put(itemUrl.getId(), List.of(itemUrl));
                    } else {
                        items.add(itemUrl);
                        resultMap.put(itemUrl.getId(), items);
                    }
                }


            }

        }

        log.warn("resourceList size: {}",resourceList.size() );

        return resultMap;

    }


}
