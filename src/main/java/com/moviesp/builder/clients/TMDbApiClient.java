package com.moviesp.builder.clients;

import com.moviesp.builder.dtos.tmdb.MovieDto;
import com.moviesp.builder.dtos.tmdb.TvShowDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TMDbApiClient {
    private final WebClient webClient;

    public TMDbApiClient(@Qualifier("tmdbWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public MovieDto getMovie(String movieId, String apiKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{movieId}")
                        .queryParam("api_key", apiKey)
                        .build(movieId))
                .retrieve()
                .bodyToMono(MovieDto.class).block();
    }

    public TvShowDto getTvShow(String tvShowId, String apiKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{tvShowId}")
                        .queryParam("api_key", apiKey)
                        .build(tvShowId))
                .retrieve()
                .bodyToMono(TvShowDto.class).block();
    }
}
