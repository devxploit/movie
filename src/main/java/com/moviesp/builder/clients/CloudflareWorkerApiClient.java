package com.moviesp.builder.clients;

import com.moviesp.builder.dtos.Folder;
import com.moviesp.builder.dtos.Movie;
import com.moviesp.builder.dtos.TvShow;
import com.moviesp.builder.dtos.cloudflare.FoldersResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CloudflareWorkerApiClient {

    private final WebClient webClient;

    public CloudflareWorkerApiClient(@Qualifier("cloudflareWorkerWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> createMovie(Movie movie) {

        return webClient.post()
                .uri("/movies")
                .bodyValue(movie)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> System.out.println("Mensaje enviado con éxito: " + response))
                .doOnError(error -> System.err.println("Error al enviar mensaje: " + error.getMessage()))
                .then();

    }

    public Mono<Void> createMovies(List<Movie> movie) {

        return webClient.post()
                .uri("/movies/bulk")
                .bodyValue(movie)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> System.out.println("Mensaje enviado con éxito: " + response))
                .doOnError(error -> System.err.println("Error al enviar mensaje: " + error.getMessage()))
                .then();

    }

    public Mono<Void> createTvshow(TvShow tvShow) {

        return webClient.post()
                .uri("/tvshows")
                .bodyValue(tvShow)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> System.out.println("Mensaje enviado con éxito: " + response))
                .doOnError(error -> System.err.println("Error al enviar mensaje: " + error.getMessage()))
                .then();

    }

    public Mono<Void> createTvshows(List<TvShow> tvShow) {

        return webClient.post()
                .uri("/tvshows/bulk")
                .bodyValue(tvShow)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> System.out.println("Mensaje enviado con éxito: " + response))
                .doOnError(error -> System.err.println("Error al enviar mensaje: " + error.getMessage()))
                .then();

    }

    public Mono<FoldersResponse> getFolders() {

        return webClient.get()
                .uri("/folders")
                .retrieve()
                .bodyToMono(FoldersResponse.class)
                .doOnError(error -> System.err.println("Error al obtener carpetas: " + error.getMessage()));

    }

    public Mono<String> updateFolder(String path, String status, String user) {

        return webClient.post()
                .uri("/folders")
                .bodyValue(new Folder(path, status, null, null, user))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> System.err.println("Error al actualizar carpeta: " + error.getMessage()));

    }
}
