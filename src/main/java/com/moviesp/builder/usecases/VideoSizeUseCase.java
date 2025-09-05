package com.moviesp.builder.usecases;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VideoSizeUseCase {

    private final Long sizeInBytes;

    public String getSize() {
        if (sizeInBytes == null) {
            return "Unknown";
        }
        double size = sizeInBytes.doubleValue();
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
