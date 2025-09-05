package com.moviesp.builder.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Configuration
public class WebClientConfig {

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Bean
    @Qualifier("tmdbWebClient")
    public WebClient tmdbWebClient( @Value("${tmdb.api.base-url}") String baseUrl,
                                    @Value("${tmdb.api.key}") String apiKey) {
        return WebClient.builder()
                .baseUrl(baseUrl) // URL base de la API
                .defaultHeader("Content-Type", "application/json")
                .filter(( request, next ) -> {
                    ClientRequest filteredRequest = ClientRequest.from(request)
                            .url(URI.create(request.url().toString().contains("?") ?
                                    request.url() + "&api_key=" + apiKey :
                                    request.url() + "?api_key=" + apiKey))
                            .build();
                    return next.exchange(filteredRequest);
                })
                .build();
    }

    @Bean
    @Qualifier("cloudflareWebClient")
    public WebClient cloudflareWebClient(
            @Value("${cloudflare.api.base-url}") String baseUrl,
            @Value("${cloudflare.api.token}") String apiToken) {

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .filter((request, next) -> next.exchange(
                        ClientRequest.from(request)
                                // Cloudflare usa 'Authorization: Bearer <TOKEN>'
                                .headers(headers -> headers.setBearerAuth(apiToken))
                                .build()
                ))
                .build();
    }

    @Bean
    @Qualifier("cloudflareWorkerWebClient")
    public WebClient cloudflareWorkerWebClient(
            @Value("${cloudflare.worker.url}") String baseUrl) {

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

}