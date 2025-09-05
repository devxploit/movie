package com.moviesp.builder.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Builder
@Data
public class Episode {
    private Integer episodeNumber;
    private String name;
    private List<Video> videos;
    private String season;
}
