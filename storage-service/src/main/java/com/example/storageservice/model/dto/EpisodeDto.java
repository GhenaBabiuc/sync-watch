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
    private List<FileInfoDto> files;
    private Integer seasonNumber;
    private Long seriesId;
    private String seriesTitle;

    public FileInfoDto getVideoFile() {
        return files != null ? files.stream()
                .filter(f -> "VIDEO".equals(f.getFileType()))
                .findFirst()
                .orElse(null) : null;
    }

    public FileInfoDto getCoverFile() {
        return files != null ? files.stream()
                .filter(f -> "COVER".equals(f.getFileType()))
                .findFirst()
                .orElse(null) : null;
    }
}
