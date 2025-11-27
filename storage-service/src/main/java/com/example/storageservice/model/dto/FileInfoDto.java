package com.example.storageservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileInfoDto {
    private Long id;
    private String fileType;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private String uploadStatus;
    private String uploadSessionId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String downloadUrl;
}
