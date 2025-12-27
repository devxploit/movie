package com.moviesp.builder.usecases;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.json.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class PublicUrlUseCase {

    private String user;
    private String token;
    private final Resource resource;

    public String getPublicUrl() {
        log.debug("Getting public URL for resource: {}", resource.getName());

        if(resource.getPublicUrl() == null){
           log.info("Resource {} does not have public URL, generating one...", resource.getName());
           return generatePublicUrl();
        } else {
            log.debug("Resource {} already has public URL: {}", resource.getName(), resource.getPublicUrl());
            return resource.getPublicUrl();
        }

    }

    String generatePublicUrl (){
        log.info("Generating public URL for resource: {} at path: {}", resource.getName(), resource.getPath().getPath());

        try {
            Credentials credentials = new Credentials(user, token);
            RestClient restClient = new RestClient(credentials);
            log.debug("Publishing resource at path: {}", resource.getPath().getPath());
            restClient.publish(resource.getPath().getPath());

            final var resourceArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
            ResourcesArgs resourcesArgs = resourceArgsUseCase.generate();


            final var resourceUseCase = new ResourceUseCase(user, token, resourcesArgs);
            Resource resource = resourceUseCase.execute();

            if(resource != null && resource.getPublicUrl() != null){
                log.info("Successfully generated public URL for resource {}: {}", resource.getName(), resource.getPublicUrl());
                return resource.getPublicUrl();
            } else {
                log.warn("Failed to generate public URL for resource: {}", resource.getName());
            }

        } catch (Exception e) {
            log.error("Error generating public URL for resource {}: {}", resource.getName(), e.getMessage(), e);
            return null;
        }

        return null;
    }
}
