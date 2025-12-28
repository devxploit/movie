package com.moviesp.builder.usecases;

import com.moviesp.builder.config.Constants;
import com.moviesp.builder.dtos.MovieItemUrl;
import com.moviesp.builder.dtos.TvshowItemUrl;
import com.yandex.disk.rest.json.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.moviesp.builder.config.Constants.*;
@Slf4j
@AllArgsConstructor
public class UrlUseCase {

    private Resource resource;

    public MovieItemUrl executeMovieUrl(){
        log.info("Processing movie URL for resource: {}", resource.getName());

        final var publicUrlUseCase = new PublicUrlUseCase(Constants.getDefaultUser(), Constants.getDefaultToken(), resource);
        String publicUrl = publicUrlUseCase.getPublicUrl();
        
        if (publicUrl == null) {
            log.warn("Failed to get public URL for movie resource: {}", resource.getName());
            return null;
        }
        
        MovieItemUrl result = processMoviePublicUrl(resource.getName(), publicUrl);
        if (result != null) {
            log.info("Successfully processed movie URL - ID: {}, Name: {}, Quality: {}", 
                    result.getId(), result.getName(), result.getQuality());
        } else {
            log.warn("Failed to process movie URL for resource: {}", resource.getName());
        }
        return result;
    }

    public TvshowItemUrl executeTvShowUrl(String name, String id){
        log.info("Processing TV show URL for resource: {} (Show: {}, ID: {})", resource.getName(), name, id);

        final var publicUrlUseCase = new PublicUrlUseCase(Constants.getDefaultUser(), Constants.getDefaultToken(), resource);
        String publicUrl = publicUrlUseCase.getPublicUrl();
        
        if (publicUrl == null) {
            log.warn("Failed to get public URL for TV show resource: {}", resource.getName());
            return null;
        }
        
        TvshowItemUrl result = processTvPublicUrl(resource.getName(), publicUrl, name, id);
        if (result != null) {
            log.info("Successfully processed TV show URL - ID: {}, Season: {}, Episode: {}", 
                    result.getId(), result.getSeason(), result.getEpisode());
        } else {
            log.warn("Failed to process TV show URL for resource: {}", resource.getName());
        }
        return result;
    }

    MovieItemUrl processMoviePublicUrl (String name, String publicUrl){
        log.debug("Processing movie public URL - Name: {}, Public URL: {}", name, publicUrl);

        String nameFormat = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
        log.debug("Detected file format: {}", nameFormat);

        String[] idSplit = name.split("id_");

        if(idSplit.length < 2){
            log.error("The file name does not contain an 'id_' segment: {}", name);
            return null;
        }
        String id = idSplit[1].replace("."+nameFormat, "").replace("."+nameFormat.toUpperCase(),"").replace("_", "").trim();
        log.debug("Extracted movie ID: {}", id);

        //Obtener calidad
        String[] nameSplit = idSplit[0].split(" ");

        String quality ="";

        for(int i = nameSplit.length-1; i >=0; i--){
            if(QUALITIES.contains(nameSplit[i].trim())){
                quality = nameSplit[i]+" "+quality;
            } else {
                break;
            }
        }
        quality = quality.trim();
        log.debug("Extracted quality: {}", quality);

        String finalName = idSplit[0].replace(quality, "").trim();
        log.info("Processed movie - ID: {}, Name: {}, Quality: {}", id, finalName, quality);

        return MovieItemUrl.builder()
                .id(id)
                .quality(quality)
                .url(publicUrl)
                .name(finalName)
                .sizeInBytes(resource.getSize())
                .size(new VideoSizeUseCase(resource.getSize()).getSize())
                .build();
    }

    TvshowItemUrl processTvPublicUrl(String name, String publicUrl, String tvName, String tvId){
        log.debug("Processing TV show public URL - Name: {}, TV Show: {}, ID: {}", name, tvName, tvId);
        
        String nameFormat = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
        log.debug("Detected file format: {}", nameFormat);

        String cleanName = name.replace("."+nameFormat, "").replace("."+nameFormat.toUpperCase(), "").trim();
        String[] seasonAndEpisode = getSeasonAndEpisode(cleanName);
        if(seasonAndEpisode == null){
            log.error("The file name does not contain a valid 'SxxExx' segment: {}", name);
            return null;
        }

        String season = seasonAndEpisode[0];
        String episode = seasonAndEpisode[1];

        log.info("Extracted TV show info - Season: {}, Episode: {}", season, episode);
        String finalName = cleanName.replace("S"+season+"E"+episode, "").trim();
        log.debug("Final episode name: {}", finalName);
        
        return TvshowItemUrl.builder()
                .id(tvId)
                .season(season)
                .episode(episode)
                .url(publicUrl)
                .name(finalName)
                .title(tvName)
                .size(new VideoSizeUseCase(resource.getSize()).getSize())
                .sizeInBytes(resource.getSize())
                .build();

    }

    String[] getSeasonAndEpisode(String nameFile){
        log.debug("Extracting season and episode from filename: {}", nameFile);

        // Regex: S(\d{1,2})E(\d{1,2})
        Pattern pattern = Pattern.compile("S(\\d{1,4})E(\\d{1,4})");

        Matcher matcher = pattern.matcher(nameFile);
        if (matcher.find()) {
            String season = matcher.group(1);
            String episode = matcher.group(2);
            log.debug("Found season {} and episode {} in filename: {}", season, episode, nameFile);

            return new String[]{season, episode};
        } else {
            log.error("Invalid format - filename does not match SxxExx pattern: {}", nameFile);
            return null;
        }

    }

}
