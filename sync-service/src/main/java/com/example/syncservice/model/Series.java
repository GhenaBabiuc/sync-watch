package com.example.syncservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Series {
    private Long id;
    private String title;
    private String description;
    private Integer year;
    private Integer totalSeasons;
    private Integer totalEpisodes;
    private List<Season> seasons;
    private String coverUrl;

    public String getCoverImageUrl() {
        return coverUrl != null ? coverUrl : "/images/default-series-cover.jpg";
    }
}
