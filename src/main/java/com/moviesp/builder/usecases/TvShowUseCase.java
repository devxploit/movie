package com.moviesp.builder.usecases;

import com.moviesp.builder.dtos.*;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TvShowUseCase {

    public TvShow build(String id, List<TvshowItemUrl> movieItemUrls) {

        Map<String, List<TvshowItemUrl>> groupedUrls = movieItemUrls.stream()
                .collect(Collectors.groupingBy(TvshowItemUrl::getSeason));


        List<Season> seasons = new java.util.ArrayList<>(List.of());

        for(Map.Entry<String, List<TvshowItemUrl>> entry : groupedUrls.entrySet()) {

            Map<String, List<TvshowItemUrl>> groupedUrlsByEpisode = movieItemUrls.stream()
                    .filter(url -> url.getSeason().equals(entry.getKey()))
                    .collect(Collectors.groupingBy(TvshowItemUrl::getEpisode));

            List<Episode> episodes = new java.util.ArrayList<>(List.of());

            for(Map.Entry<String, List<TvshowItemUrl>> entryEpisode : groupedUrlsByEpisode.entrySet()) {

                List<Video> videos = entryEpisode.getValue().stream()
                        .map(url -> Video.builder()
                                .resolution(url.getQuality())
                                .url(url.getUrl())
                                .size(url.getSize())
                                .sizeInBytes(url.getSizeInBytes())
                                .createdAt(new DateUseCase().getNowDate())
                                .build())
                        .toList();

                Episode episode = Episode.builder()
                        .episodeNumber( parseIntOrDefault(entryEpisode.getKey()) )
                        .name("Episodio " + entryEpisode.getKey())
                        .videos(videos)
                        .season(entry.getKey())
                        .build();

                episodes.add(episode);

            }

            Season season = Season.builder()
                    .episodes(episodes)
                    .name(entry.getKey())
                    .userScore(0.0)
                    .date(new DateUseCase().getNowDate())
                    .createdAt(new DateUseCase().getNowDate())
                    .build();
            seasons.add(season);

        }

        return TvShow.builder()
                .id(id)
                .seasons(seasons)
                .updatedAt(new DateUseCase().getNowDate())
                .genres(List.of())
                .name(movieItemUrls.get(0).getName())
                .aliases(List.of(movieItemUrls.get(0).getName()))
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
