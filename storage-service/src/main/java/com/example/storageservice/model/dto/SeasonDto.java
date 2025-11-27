package com.example.storageservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeasonDto {
    private Long id;
    private Long seriesId;
    private Integer seasonNumber;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private Integer totalEpisodes;
    private List<EpisodeDto> episodes;
    private String seriesTitle;
}
