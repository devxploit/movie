package com.moviesp.builder.usecases;

import com.moviesp.builder.dtos.Movie;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class EnrichUseCase {

    private final String type;
    private final String id;

    @Value("${cloudflare.worker.url}")
    private String workerUrl;

    private final WebClient webClient;

    public Mono<String> createMovie(Movie movie) {
        return webClient.post()
                .uri("/entries")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken) // Descomenta si usas token
                .body(Mono.just(movie), Movie.class)
                .retrieve()
                .bodyToMono(String.class);
    }


}
