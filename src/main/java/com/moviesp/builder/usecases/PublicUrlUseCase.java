package com.moviesp.builder.usecases;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.json.Resource;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PublicUrlUseCase {

    private String user;
    private String token;
    private final Resource resource;

    public String getPublicUrl() {

        if(resource.getPublicUrl() == null){
           return generatePublicUrl();
        } else {
            return resource.getPublicUrl();
        }

    }

    String generatePublicUrl (){

        try {
            Credentials credentials = new Credentials(user, token);
            RestClient restClient = new RestClient(credentials);
            restClient.publish(resource.getPath().getPath());

            final var resourceArgsUseCase = new ResourceArgsUseCase(resource.getPath().getPath());
            ResourcesArgs resourcesArgs = resourceArgsUseCase.generate();


            final var resourceUseCase = new ResourceUseCase(user, token, resourcesArgs);
            Resource resource = resourceUseCase.execute();

            if(resource != null){
                return resource.getPublicUrl();
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }
}
