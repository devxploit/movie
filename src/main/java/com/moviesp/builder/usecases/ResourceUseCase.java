package com.moviesp.builder.usecases;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.json.Resource;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResourceUseCase {

    private String user;
    private String token;
    private ResourcesArgs resourcesArgs;
    public Resource execute(){

        Credentials credentials = new Credentials(user, token);
        RestClient restClient = new RestClient(credentials);

        try {
            return restClient.getResources(resourcesArgs);
        } catch (Exception e) {
            return null;
        }


    }
}
