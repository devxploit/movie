package com.moviesp.builder.usecases;

import com.moviesp.builder.dtos.Movie;
import com.moviesp.builder.dtos.MovieItemUrl;
import com.moviesp.builder.dtos.Video;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class MovieUseCase {

    public Movie build(String id, List<MovieItemUrl> movieItemUrls){
        log.info("Building Movie object - ID: {}, URLs count: {}", id, movieItemUrls.size());

        List<Video> videos = mapToVideos(movieItemUrls);
        log.debug("Created {} videos for movie ID: {}", videos.size(), id);

        String title = movieItemUrls.get(0).getName();
        log.info("Building movie - ID: {}, Title: {}, Videos: {}", id, title, videos.size());

        return Movie.builder()
                .id(id)
                .title(title)
                .userScore(0.0)
                .aliases(List.of(title))
                .videos(videos)
                .genres(List.of())
                .build();

    }

    private List<Video> mapToVideos(List<MovieItemUrl> movieItemUrls){
        log.debug("Mapping {} movie item URLs to Video objects", movieItemUrls.size());

        return movieItemUrls.stream().map(movieItemUrl -> {
            log.debug("Creating video - URL: {}, Resolution: {}, Size: {}", 
                    movieItemUrl.getUrl(), movieItemUrl.getQuality(), movieItemUrl.getSize());
            return Video.builder()
                    .url(movieItemUrl.getUrl())
                    .resolution(movieItemUrl.getQuality())
                    .size(movieItemUrl.getSize())
                    .sizeInBytes(movieItemUrl.getSizeInBytes())
                    .createdAt(new DateUseCase().getNowDate())
                    .build();
        }).toList();
    }

}
