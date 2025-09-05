package com.moviesp.builder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class Constants {

    @Value("${yandex.disk.legacy.user}")
    public static String LEGACY_USER;
    @Value("${yandex.disk.legacy.token}")
    public static String LEGACY_TOKEN;
    @Value("${yandex.disk.updates.user}")
    public String UPDATES_USER;
    @Value("${yandex.disk.updates.token}")
    public String UPDATES_TOKEN;
    public static final String DEFAULT_USER = LEGACY_USER;
    public static final String DEFAULT_TOKEN = LEGACY_TOKEN;
    public static final List<String> QUALITIES = Arrays.asList(
            "HD", "FullHD", "4K", "8K", "CAM", "4k", "8k", "TS", "TC", "HDR", "UHD", "SCR", "DVDRip", "HDRip", "BDRip", "WEB-DL",
            "WEBRip", "HDTV", "PDTV", "SDTV", "DVD", "VHS", "TVRip", "R5", "HD-TS", "HD-TC", "HD-SCR", "HD-DVDRip", "HD-HDRip", "HD-BDRip",
            "HD-WEB-DL", "HD-WEBRip", "HD-HDTV", "HD-PDTV", "HD-SDTV", "720", "1080", "2160", "4320", "720p", "1080p", "2160p", "4320p",
            "DUAL", "SUB", "MULTI", "DUALC", "CAST", "EXT", "REMASTER", "UNCUT", "LIMITED", "DIRECTORS", "CUT", "UNRATED", "THEATRICAL", "SPECIAL", "EDITION",
            "LAT", "ESP", "VOSE", "VO", "SUBTITULADA", "SUBTITULADO", "CASTELLANO", "ESPAÑOL", "TRIAL", "CNCT", "60FPS", "120FPS", "3D", "IMAX", "VR", "EXTENDED", "SDR",
            "Remux", "60fps", "120fps", "4df"
    );


}
