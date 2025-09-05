package com.moviesp.builder.dtos.tmdb;

import java.time.LocalDate;
import java.util.List;

public record MovieDto(
        int id,
        String title,
        String overview,
        String poster_path,
        LocalDate release_date,
        double vote_average,
        List<GenreDto> genres
) {}