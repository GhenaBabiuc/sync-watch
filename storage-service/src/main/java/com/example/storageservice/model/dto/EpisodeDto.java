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
public class EpisodeDto {
    private Long id;
    private Long seasonId;
    private Integer episodeNumber;
    private String title;
    private String description;
    private Integer duration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MediaDto> mediaFiles;
}
