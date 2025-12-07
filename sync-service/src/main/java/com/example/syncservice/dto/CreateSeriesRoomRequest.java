package com.example.syncservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSeriesRoomRequest {
    @NotBlank
    private String roomName;

    @NotBlank
    private String userId;

    @NotNull
    private Long seriesId;

    @NotBlank
    private String title;

    @NotNull
    private Long seasonId;

    @NotNull
    private Long episodeId;
}
