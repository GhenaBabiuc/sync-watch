package com.example.storageservice.config;

import com.example.storageservice.model.Episode;
import com.example.storageservice.model.EpisodeMedia;
import com.example.storageservice.model.MediaCategory;
import com.example.storageservice.model.MediaFile;
import com.example.storageservice.model.Movie;
import com.example.storageservice.model.MovieMedia;
import com.example.storageservice.model.Season;
import com.example.storageservice.model.SeasonMedia;
import com.example.storageservice.model.Series;
import com.example.storageservice.model.SeriesMedia;
import com.example.storageservice.model.dto.EpisodeDto;
import com.example.storageservice.model.dto.MediaDto;
import com.example.storageservice.model.dto.MovieDto;
import com.example.storageservice.model.dto.SeasonDto;
import com.example.storageservice.model.dto.SeriesDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        mapper.addMappings(new PropertyMap<Movie, MovieDto>() {
            @Override
            protected void configure() {
                using(ctx -> ((List<MovieMedia>) ctx.getSource()).stream()
                        .map(mm -> mapMediaFile(mm.getMediaFile(), mm.getCategory(), mm.isPrimary()))
                        .toList())
                        .map(source.getMedia(), destination.getMediaFiles());
            }
        });

        mapper.addMappings(new PropertyMap<Series, SeriesDto>() {
            @Override
            protected void configure() {
                using(ctx -> ((List<SeriesMedia>) ctx.getSource()).stream()
                        .map(sm -> mapMediaFile(sm.getMediaFile(), sm.getCategory(), sm.isPrimary()))
                        .toList())
                        .map(source.getMedia(), destination.getMediaFiles());
            }
        });

        mapper.addMappings(new PropertyMap<Season, SeasonDto>() {
            @Override
            protected void configure() {
                map().setSeriesId(source.getSeriesId());

                using(ctx -> ((List<SeasonMedia>) ctx.getSource()).stream()
                        .map(sm -> mapMediaFile(sm.getMediaFile(), sm.getCategory(), sm.isPrimary()))
                        .toList())
                        .map(source.getMedia(), destination.getMediaFiles());
            }
        });

        mapper.addMappings(new PropertyMap<Episode, EpisodeDto>() {
            @Override
            protected void configure() {
                map().setSeasonId(source.getSeasonId());

                using(ctx -> ((List<EpisodeMedia>) ctx.getSource()).stream()
                        .map(em -> mapMediaFile(em.getMediaFile(), em.getCategory(), em.isPrimary()))
                        .toList())
                        .map(source.getMedia(), destination.getMediaFiles());
            }
        });

        return mapper;
    }

    private MediaDto mapMediaFile(MediaFile file, MediaCategory category, boolean isPrimary) {
        if (file == null) return null;
        return MediaDto.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .category(category)
                .isPrimary(isPrimary)
                .uploadStatus(file.getUploadStatus())
                .createdAt(file.getCreatedAt())
                .build();
    }
}
