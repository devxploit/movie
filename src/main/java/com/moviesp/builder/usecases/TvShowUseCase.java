package com.moviesp.builder.usecases;

import com.moviesp.builder.dtos.*;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TvShowUseCase {

    public TvShow build(String id, List<TvshowItemUrl> movieItemUrls) {
        log.info("Building TV Show object - ID: {}, URLs count: {}", id, movieItemUrls.size());

        Map<String, List<TvshowItemUrl>> groupedUrls = movieItemUrls.stream()
                .collect(Collectors.groupingBy(TvshowItemUrl::getSeason));
        
        log.info("Grouped URLs into {} seasons", groupedUrls.size());


        List<Season> seasons = new java.util.ArrayList<>(List.of());

        for(Map.Entry<String, List<TvshowItemUrl>> entry : groupedUrls.entrySet()) {
            log.info("Processing season {} with {} URLs", entry.getKey(), entry.getValue().size());

            Map<String, List<TvshowItemUrl>> groupedUrlsByEpisode = movieItemUrls.stream()
                    .filter(url -> url.getSeason().equals(entry.getKey()))
                    .collect(Collectors.groupingBy(TvshowItemUrl::getEpisode));

            log.debug("Season {} has {} episodes", entry.getKey(), groupedUrlsByEpisode.size());

            List<Episode> episodes = new java.util.ArrayList<>(List.of());

            for(Map.Entry<String, List<TvshowItemUrl>> entryEpisode : groupedUrlsByEpisode.entrySet()) {
                log.debug("Processing episode {} of season {} with {} videos", 
                        entryEpisode.getKey(), entry.getKey(), entryEpisode.getValue().size());

                List<Video> videos = entryEpisode.getValue().stream()
                        .map(url -> {
                            log.debug("Creating video for episode - URL: {}, Resolution: {}", 
                                    url.getUrl(), url.getQuality());
                            return Video.builder()
                                    .resolution(url.getQuality())
                                    .url(url.getUrl())
                                    .size(url.getSize())
                                    .sizeInBytes(url.getSizeInBytes())
                                    .createdAt(new DateUseCase().getNowDate())
                                    .build();
                        })
                        .toList();

                Episode episode = Episode.builder()
                        .episodeNumber( parseIntOrDefault(entryEpisode.getKey()) )
                        .name("Episodio " + entryEpisode.getKey())
                        .videos(videos)
                        .season(entry.getKey())
                        .build();

                episodes.add(episode);
                log.debug("Created episode {} with {} videos", entryEpisode.getKey(), videos.size());

            }

            Season season = Season.builder()
                    .episodes(episodes)
                    .name(entry.getKey())
                    .userScore(0.0)
                    .date(new DateUseCase().getNowDate())
                    .createdAt(new DateUseCase().getNowDate())
                    .build();
            seasons.add(season);
            log.info("Created season {} with {} episodes", entry.getKey(), episodes.size());

        }

        String tvShowName = movieItemUrls.get(0).getName();
        log.info("Building TV Show - ID: {}, Name: {}, Seasons: {}", id, tvShowName, seasons.size());
        
        return TvShow.builder()
                .id(id)
                .seasons(seasons)
                .updatedAt(new DateUseCase().getNowDate())
                .genres(List.of())
                .name(tvShowName)
                .aliases(List.of(tvShowName))
                .date(new DateUseCase().getNowDate())
                .createdAt(new DateUseCase().getNowDate())
                .build();
    }

    private Episode buildEpisode(String season, List<TvshowItemUrl> urls) {

        List<Video> videos = urls.stream()
                .map(url -> Video.builder()
                        .resolution(url.getQuality())
                        .url(url.getUrl())
                        .size(url.getSize())
                        .sizeInBytes(url.getSizeInBytes())
                        .createdAt(new DateUseCase().getNowDate())
                        .build())
                .toList();

        return Episode.builder()
                .episodeNumber( parseIntOrDefault(urls.get(0).getEpisode()) )
                .name("Episodio " + urls.get(0).getEpisode())
                .videos(videos)
                .season(season)
                .build();

    }

    private int parseIntOrDefault(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


}
