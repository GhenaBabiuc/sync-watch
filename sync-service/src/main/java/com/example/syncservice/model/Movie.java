package com.example.syncservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    private Long id;
    private String title;
    private String description;
    private Integer year;
    private Integer duration;
    private String coverUrl;
    private String streamUrl;
    private List<FileInfo> files;

    public String getFormattedDuration() {
        if (duration == null) return "0:00";
        long hours = duration / 60;
        long minutes = duration % 60;
        if (hours > 0) {
            return String.format("%d:%02d:00", hours, minutes);
        } else {
            return String.format("%d:00", minutes);
        }
    }

    public String getCoverImageUrl() {
        if (coverUrl != null) return coverUrl;

        if (files != null) {
            return files.stream()
                    .filter(f -> "COVER".equals(f.getFileType()))
                    .map(FileInfo::getDownloadUrl)
                    .findFirst()
                    .orElse("/images/default-movie-cover.jpg");
        }

        return "/images/default-movie-cover.jpg";
    }
}
