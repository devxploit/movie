package com.moviesp.builder.usecases;

import com.moviesp.builder.dtos.Movie;
import com.moviesp.builder.dtos.MovieItemUrl;
import com.moviesp.builder.dtos.Video;

import java.util.List;

public class MovieUseCase {

    public Movie build(String id, List<MovieItemUrl> movieItemUrls){

        List<Video> videos = mapToVideos(movieItemUrls);

        return Movie.builder()
                .id(id)
                .title(movieItemUrls.get(0).getName())
                .userScore(0.0)
                .aliases(List.of(movieItemUrls.get(0).getName()))
                .videos(videos)
                .genres(List.of())
                .build();

    }

    private List<Video> mapToVideos(List<MovieItemUrl> movieItemUrls){

        return movieItemUrls.stream().map(movieItemUrl -> Video.builder()
                .url(movieItemUrl.getUrl())
                .resolution(movieItemUrl.getQuality())
                .size(movieItemUrl.getSize())
                .sizeInBytes(movieItemUrl.getSizeInBytes())
                .createdAt(new DateUseCase().getNowDate())
                .build()).toList();
    }

}
