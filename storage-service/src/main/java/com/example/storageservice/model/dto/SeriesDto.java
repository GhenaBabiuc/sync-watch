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
public class SeriesDto {
    private Long id;
    private String title;
    private String description;
    private Integer year;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalSeasons;
    private Integer totalEpisodes;
    private List<SeasonDto> seasons;
    private List<MediaDto> mediaFiles;
}
