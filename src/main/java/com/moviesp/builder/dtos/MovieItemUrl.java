package com.moviesp.builder.dtos;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class MovieItemUrl {

    private String url;
    private String quality;
    private String id;
    private String name;
    private String size;
    private Long sizeInBytes;

}
