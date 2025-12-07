package com.example.syncservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMovieRoomRequest {
    @NotBlank(message = "Room name is required")
    private String roomName;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Movie ID is required")
    private Long movieId;

    @NotBlank
    private String title;
}
