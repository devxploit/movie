package com.moviesp.builder.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Builder
@Data
public class TvShow {
    private String id;
    private String date;
    private String createdAt;
    private String updatedAt;
    private List<Genre> genres;
    private String name;
    private List<String> aliases;
    private List<Season> seasons;


}
