package com.example.storageservice.controller;

import com.example.storageservice.model.dto.FileInfoDto;
import com.example.storageservice.model.dto.FileUploadRequest;
import com.example.storageservice.model.dto.FileUploadResponse;
import com.example.storageservice.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Upload Controller", description = "API for managing file uploads")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "Initiate file upload", description = "Creates upload session and returns presigned URL")
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> initiateFileUpload(@Valid @RequestBody FileUploadRequest request) {
        log.info("Initiating {} upload for {} ID: {}, file: {}",
                request.getFileType(), request.getEntityType(), request.getEntityId(), request.getOriginalFilename());

        FileUploadResponse response = fileUploadService.initiateFileUpload(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get files by movie", description = "Retrieves all files for a specific movie")
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<FileInfoDto>> getMovieFiles(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long movieId) {

        List<FileInfoDto> files = fileUploadService.getFilesByMovie(movieId);
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "Get files by episode", description = "Retrieves all files for a specific episode")
    @GetMapping("/episode/{episodeId}")
    public ResponseEntity<List<FileInfoDto>> getEpisodeFiles(
            @Parameter(description = "Episode ID", required = true) @PathVariable Long episodeId) {

        List<FileInfoDto> files = fileUploadService.getFilesByEpisode(episodeId);
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "Delete file", description = "Deletes a file and removes it from storage")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "File ID", required = true) @PathVariable Long fileId,
            @Parameter(description = "Entity type", required = true) @RequestParam FileUploadRequest.EntityType entityType) {

        log.info("Deleting file {} of entity type {}", fileId, entityType);
        fileUploadService.deleteFile(fileId, entityType);
        return ResponseEntity.noContent().build();
    }
}
