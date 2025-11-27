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

    public String getCoverImageUrl() {
        if (seasons != null && !seasons.isEmpty()) {
            for (Season season : seasons) {
                if (season.getEpisodes() != null && !season.getEpisodes().isEmpty()) {
                    for (Episode episode : season.getEpisodes()) {
                        String episodeCover = episode.getCoverImageUrl();
                        if (episodeCover != null && !episodeCover.equals("/images/default-episode-cover.jpg")) {
                            return episodeCover;
                        }
                    }
                }
            }
        }

        return "/images/default-series-cover.jpg";
    }
}
