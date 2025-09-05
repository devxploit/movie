package com.moviesp.builder.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Builder
@Data
public class Movie {

    private String id;
    private String date;
    private String createdAt;
    private String updatedAt;
    private String title;
    private List<Genre> genres;
    private Double userScore;
    private List<String> aliases;

    private List<Video> videos;

}
