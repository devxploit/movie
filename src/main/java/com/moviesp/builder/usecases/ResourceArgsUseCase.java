package com.moviesp.builder.usecases;

import com.yandex.disk.rest.ResourcesArgs;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResourceArgsUseCase {

    private String path;

    public ResourcesArgs generate(){
        return new ResourcesArgs.Builder()
                .setPath(path)
                .setLimit(100000)
                .setPreviewCrop(false).build();
    }
}
