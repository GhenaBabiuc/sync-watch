package com.example.storageservice.model.dto;

import com.example.storageservice.model.EntityType;
import com.example.storageservice.model.MediaCategory;
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

    private String originalFilename;
    private String mimeType;

    @NotNull
    @Positive
    private Long fileSize;

    @NotNull
    @Positive
    private Long entityId;

    @NotNull
    private EntityType entityType;

    @NotNull
    private MediaCategory category;

    private boolean isPrimary;
}
