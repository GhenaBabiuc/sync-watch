package com.example.storageservice.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateEpisodeRequest {

    @NotNull(message = "Season ID is required")
    @Positive(message = "Season ID must be positive")
    private Long seasonId;

    @NotNull(message = "Episode number is required")
    @Min(value = 1, message = "Episode number must be at least 1")
    @Max(value = 500, message = "Episode number cannot be greater than 500")
    private Integer episodeNumber;

    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @Positive(message = "Duration must be positive")
    private Integer duration;
}
