package com.moviesp.builder.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Getter
@Configuration
public class Constants {

    @Getter
    private static String legacyUser;
    @Getter
    private static String legacyToken;
    
    @Value("${yandex.disk.legacy.user:}")
    public void setLegacyUser(String legacyUser) {
        Constants.legacyUser = legacyUser;
    }
    
    @Value("${yandex.disk.legacy.token:}")
    public void setLegacyToken(String legacyToken) {
        Constants.legacyToken = legacyToken;
    }

    @Value("${yandex.disk.updates.user:}")
    private String updatesUser;
    
    @Value("${yandex.disk.updates.token:}")
    private String updatesToken;

    public static String getDefaultUser() {
        return getLegacyUser();
    }
    
    public static String getDefaultToken() {
        return getLegacyToken();
    }
    public static final List<String> QUALITIES = Arrays.asList(
            "HD", "FullHD", "4K", "8K", "CAM", "4k", "8k", "TS", "TC", "HDR", "UHD", "SCR", "DVDRip", "HDRip", "BDRip", "WEB-DL",
            "WEBRip", "HDTV", "PDTV", "SDTV", "DVD", "VHS", "TVRip", "R5", "HD-TS", "HD-TC", "HD-SCR", "HD-DVDRip", "HD-HDRip", "HD-BDRip",
            "HD-WEB-DL", "HD-WEBRip", "HD-HDTV", "HD-PDTV", "HD-SDTV", "720", "1080", "2160", "4320", "720p", "1080p", "2160p", "4320p",
            "DUAL", "SUB", "MULTI", "DUALC", "CAST", "EXT", "REMASTER", "UNCUT", "LIMITED", "DIRECTORS", "CUT", "UNRATED", "THEATRICAL", "SPECIAL", "EDITION",
            "LAT", "ESP", "VOSE", "VO", "SUBTITULADA", "SUBTITULADO", "CASTELLANO", "ESPAÑOL", "TRIAL", "CNCT", "60FPS", "120FPS", "3D", "IMAX", "VR", "EXTENDED", "SDR",
            "Remux", "60fps", "120fps", "4df", "Mkv"
    );


}
