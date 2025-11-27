package com.example.storageservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaInfoDto {
    private Long id;
    private String type;
    private String title;
    private String description;
    private Integer year;
    private Integer duration;
    private String streamUrl;
    private Integer episodeNumber;
    private Long seasonId;
    private Integer seasonNumber;
    private Long seriesId;
    private String seriesTitle;
}
