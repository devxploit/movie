package com.moviesp.builder.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Video {
    private String resolution;
    private String url;
    private String size;
    private Long sizeInBytes;
    private String createdAt;

}
