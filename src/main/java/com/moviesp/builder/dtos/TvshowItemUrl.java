package com.moviesp.builder.dtos;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class TvshowItemUrl extends MovieItemUrl {

    private String season;
    private String episode;
    private String title;

}
