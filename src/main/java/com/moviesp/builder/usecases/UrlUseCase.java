package com.moviesp.builder.usecases;

import com.moviesp.builder.dtos.MovieItemUrl;
import com.moviesp.builder.dtos.TvshowItemUrl;
import com.yandex.disk.rest.json.Resource;
import lombok.AllArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.moviesp.builder.config.Constants.*;
@AllArgsConstructor
public class UrlUseCase {

    private Resource resource;

    public MovieItemUrl executeMovieUrl(){

        final var publicUrlUseCase = new PublicUrlUseCase(DEFAULT_USER, DEFAULT_TOKEN, resource);
        String publicUrl = publicUrlUseCase.getPublicUrl();
        return processMoviePublicUrl(resource.getName(), publicUrl);
    }

    public TvshowItemUrl executeTvShowUrl(String name, String id){

        final var publicUrlUseCase = new PublicUrlUseCase(DEFAULT_USER, DEFAULT_TOKEN, resource);
        String publicUrl = publicUrlUseCase.getPublicUrl();
        return processTvPublicUrl(resource.getName(), publicUrl, name, id);
    }

    MovieItemUrl processMoviePublicUrl (String name, String publicUrl){

        String nameFormat = name.substring(name.lastIndexOf(".") + 1).toLowerCase();

        String[] idSplit = name.split("id_");

        if(idSplit.length < 2){
            //log.error("   - The file name does not contain an 'id_' segment: {}", name);
            return null;
        }
        String id = idSplit[1].replace("."+nameFormat, "").replace("."+nameFormat.toUpperCase(),"").trim();

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

        String finalName = idSplit[0].replace(quality, "").trim();
        //log.warn("   - Quality: {} - Name: {}", quality, finalName);

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
        String nameFormat = name.substring(name.lastIndexOf(".") + 1).toLowerCase();

        String cleanName = name.replace("."+nameFormat, "").replace("."+nameFormat.toUpperCase(), "").trim();
        String[] seasonAndEpisode = getSeasonAndEpisode(cleanName);
        if(seasonAndEpisode == null){
            //log.error("   - The file name does not contain a valid 'SxxExx' segment: {}", name);
            return null;
        }

        String season = seasonAndEpisode[0];
        String episode = seasonAndEpisode[1];

        //log.warn("   - Season: {} - Episode: {}", season, episode);
        String finalName = cleanName.replace("S"+season+"E"+episode, "").trim();
        //log.warn("   - Name: {}", finalName);
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

        // Regex: S(\d{1,2})E(\d{1,2})
        Pattern pattern = Pattern.compile("S(\\d{1,4})E(\\d{1,4})");

        Matcher matcher = pattern.matcher(nameFile);
        if (matcher.find()) {
            String season = matcher.group(1);
            String episode = matcher.group(2);
            //System.out.println("Input: " + nameFile +
              //      " -> Temporada: " + season +
                //    ", Episodio: " + episode);

            return new String[]{season, episode};
        } else {

            return null;
            //log.error("Formato no válido: " + nameFile);
        }

    }

}
