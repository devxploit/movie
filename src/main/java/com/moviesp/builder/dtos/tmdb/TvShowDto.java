package com.moviesp.builder.dtos.tmdb;

import java.time.LocalDate;
import java.util.List;
public record TvShowDto(
        int id,
        String name,
        String overview,
        String poster_path,
        LocalDate first_air_date,
        double vote_average,
        List<GenreDto> genres
) {}