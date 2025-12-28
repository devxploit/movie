package com.moviesp.builder.services.explorer;

import com.moviesp.builder.config.Constants;
import com.moviesp.builder.usecases.ResourceArgsUseCase;
import com.moviesp.builder.usecases.ResourceUseCase;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.json.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderExplorerService {

    private final Constants constants;

    public List<ExplorerItem> listItems(String path, String generator) {
        String user = getUser(generator);
        String token = getToken(generator);

        log.info("Listing items for path: {} with generator: {}", path, generator);

        final var resourceArgsUseCase = new ResourceArgsUseCase(path);
        ResourcesArgs resourcesArgs = resourceArgsUseCase.generate();

        try {
            final var resourceUseCase = new ResourceUseCase(user, token, resourcesArgs);
            Resource resource = resourceUseCase.execute();

            if (resource == null || resource.getResourceList() == null) {
                return Collections.emptyList();
            }

            return resource.getResourceList().getItems().stream()
                    .map(this::mapToExplorerItem)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error listing items for path: {}", path, e);
            throw new RuntimeException("Failed to list items from Yandex Disk", e);
        }
    }

    private ExplorerItem mapToExplorerItem(Resource resource) {
        return new ExplorerItem(
                resource.getName(),
                resource.getPath().getPath(),
                resource.getType(),
                resource.getSize());
    }

    private String getUser(String generator) {
        if ("updates".equalsIgnoreCase(generator)) {
            return constants.getUpdatesUser();
        }
        return Constants.getLegacyUser();
    }

    private String getToken(String generator) {
        if ("updates".equalsIgnoreCase(generator)) {
            return constants.getUpdatesToken();
        }
        return Constants.getLegacyToken();
    }

    public record ExplorerItem(String name, String path, String type, long size) {
    }
}
