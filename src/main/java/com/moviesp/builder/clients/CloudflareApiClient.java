package com.moviesp.builder.clients;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CloudflareApiClient {

    private final WebClient webClient;
    @Value("${cloudflare.account.id}") String accountId;
    @Value("${cloudflare.queue.id}") String queueId;

    public CloudflareApiClient(@Qualifier("cloudflareWebClient") WebClient webClient) {
        this.webClient = webClient;
    }


    public Mono<Void> publishUpdateTask(String type, int tmdbId, String recordId) {
        String uri = String.format("/accounts/%s/queues/%s/messages", accountId, queueId);

        MessagePayload payload = new MessagePayload(type, tmdbId, recordId);
        MessageWrapper wrapper = new MessageWrapper(payload);

        return webClient.post()
                .uri(uri)
                .bodyValue(wrapper)
                .retrieve()
                .bodyToMono(String.class) // Se puede mapear a un DTO de respuesta si es necesario
                .doOnSuccess(response -> System.out.println("Mensaje enviado con éxito: " + response))
                .doOnError(error -> System.err.println("Error al enviar mensaje: " + error.getMessage()))
                .then();
    }

    private record MessageWrapper(MessagePayload body) {}
    private record MessagePayload(String type, int tmdbId, String recordId) {}
}