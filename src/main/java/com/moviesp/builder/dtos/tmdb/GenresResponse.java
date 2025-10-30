package com.moviesp.builder.dtos.tmdb;

import java.util.List;

public record GenresResponse(
        List<GenreDto> genres
) {}

