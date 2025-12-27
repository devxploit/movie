package com.moviesp.builder.usecases;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.json.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ResourceUseCase {

    private String user;
    private String token;
    private ResourcesArgs resourcesArgs;
    public Resource execute(){
        log.info("Executing ResourceUseCase - Fetching resources from path: {}", resourcesArgs.getPath());
        
        Credentials credentials = new Credentials(user, token);
        RestClient restClient = new RestClient(credentials);

        try {
            Resource resource = restClient.getResources(resourcesArgs);
            if (resource != null) {
                log.info("Successfully fetched resource: {} (Type: {})", resource.getName(), resource.getType());
                if (resource.getResourceList() != null && resource.getResourceList().getItems() != null) {
                    log.info("Resource contains {} items", resource.getResourceList().getItems().size());
                }
            } else {
                log.warn("ResourceUseCase returned null for path: {}", resourcesArgs.getPath());
            }
            return resource;
        } catch (Exception e) {
            log.error("Error fetching resources from path {}: {}", resourcesArgs.getPath(), e.getMessage(), e);
            return null;
        }


    }
}
