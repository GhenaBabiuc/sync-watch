package com.example.storageservice.model.dto;

import com.example.storageservice.model.EpisodeFile;
import com.example.storageservice.model.MovieFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileUploadRequest {

    @NotBlank(message = "Original filename cannot be blank")
    private String originalFilename;

    @NotBlank(message = "MIME type cannot be blank")
    private String mimeType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    @NotNull(message = "Entity ID is required")
    @Positive(message = "Entity ID must be positive")
    private Long entityId;

    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    @NotNull(message = "File type is required")
    private String fileType;

    public enum EntityType {
        MOVIE, EPISODE
    }

    public MovieFile.FileType getMovieFileType() {
        return MovieFile.FileType.valueOf(fileType.toUpperCase());
    }

    public EpisodeFile.FileType getEpisodeFileType() {
        return EpisodeFile.FileType.valueOf(fileType.toUpperCase());
    }
}
