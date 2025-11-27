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
public class FileUploadResponse {
    private String uploadSessionId;
    private String presignedUrl;
    private LocalDateTime expiresAt;
    private String minioObjectKey;
}
