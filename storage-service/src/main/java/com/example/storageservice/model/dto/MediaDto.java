package com.example.storageservice.model.dto;

import com.example.storageservice.model.MediaCategory;
import com.example.storageservice.model.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaDto {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private MediaCategory category;
    private boolean isPrimary;
    private UploadStatus uploadStatus;
    private LocalDateTime createdAt;
}
