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
public class CreateSeasonRequest {

    @NotNull(message = "Series ID is required")
    @Positive(message = "Series ID must be positive")
    private Long seriesId;

    @NotNull(message = "Season number is required")
    @Min(value = 1, message = "Season number must be at least 1")
    @Max(value = 50, message = "Season number cannot be greater than 50")
    private Integer seasonNumber;

    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;
}
