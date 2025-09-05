package com.moviesp.builder.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Builder
@Data
public class Season {
    private String name;
    private String date;
    private String createdAt;
    private Double userScore;
    private List<Episode> episodes;
}
