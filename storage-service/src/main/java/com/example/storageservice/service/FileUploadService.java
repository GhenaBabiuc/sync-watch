package com.example.storageservice.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.storageservice.config.MinioProperties;
import com.example.storageservice.exception.MovieNotFoundException;
import com.example.storageservice.model.EpisodeFile;
import com.example.storageservice.model.MovieFile;
import com.example.storageservice.model.dto.FileInfoDto;
import com.example.storageservice.model.dto.FileUploadRequest;
import com.example.storageservice.model.dto.FileUploadResponse;
import com.example.storageservice.repository.EpisodeFileRepository;
import com.example.storageservice.repository.EpisodeRepository;
import com.example.storageservice.repository.MovieFileRepository;
import com.example.storageservice.repository.MovieRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final MovieFileRepository movieFileRepository;
    private final EpisodeFileRepository episodeFileRepository;

    private static final long UPLOAD_EXPIRY_HOURS = 24;

    @Transactional
    public FileUploadResponse initiateFileUpload(FileUploadRequest request) {
        validateUploadRequest(request);

        String uploadSessionId = UUID.randomUUID().toString();
        String objectKey = generateObjectKey(request);

        try {
            String presignedUrl = generatePresignedUrl(objectKey);
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(UPLOAD_EXPIRY_HOURS);

            switch (request.getEntityType()) {
                case MOVIE -> createMovieFileRecord(request, uploadSessionId, objectKey, presignedUrl, expiresAt);
                case EPISODE -> createEpisodeFileRecord(request, uploadSessionId, objectKey, presignedUrl, expiresAt);
            }

            log.info("File upload initiated: {} for {} ID: {}, uploadSessionId: {}",
                    request.getOriginalFilename(), request.getEntityType(),
                    request.getEntityId(), uploadSessionId);

            return FileUploadResponse.builder()
                    .uploadSessionId(uploadSessionId)
                    .presignedUrl(presignedUrl)
                    .expiresAt(expiresAt)
                    .minioObjectKey(objectKey)
                    .build();

        } catch (Exception e) {
            log.error("Error initiating file upload for {}: {}", request.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to initiate file upload", e);
        }
    }

    @Async
    @Transactional
    public void handleFileUploadCompletion(String bucket, String objectKey) {
        log.info("Processing file upload completion for object: {}", objectKey);

        movieFileRepository.findByMinioLocation(bucket, objectKey)
                .ifPresentOrElse(this::markMovieFileAsCompleted, () ->
                        episodeFileRepository.findByMinioLocation(bucket, objectKey)
                                .ifPresentOrElse(this::markEpisodeFileAsCompleted, () ->
                                        log.warn("No file record found for completed upload: {}", objectKey)));
    }

    public List<FileInfoDto> getFilesByMovie(Long movieId) {
        List<MovieFile> files = movieFileRepository.findByMovieId(movieId);
        return files.stream().map(this::mapMovieFileToDto).toList();
    }

    public List<FileInfoDto> getFilesByEpisode(Long episodeId) {
        List<EpisodeFile> files = episodeFileRepository.findByEpisodeId(episodeId);
        return files.stream().map(this::mapEpisodeFileToDto).toList();
    }

    @Transactional
    public void deleteFile(Long fileId, FileUploadRequest.EntityType entityType) {
        switch (entityType) {
            case MOVIE -> {
                MovieFile file = movieFileRepository.findById(fileId)
                        .orElseThrow(() -> new MovieNotFoundException("Movie file not found: " + fileId));
                deleteFileFromStorage(file.getMinioObjectKey());
                movieFileRepository.delete(file);
                log.info("Movie file deleted: {} ({})", file.getOriginalFilename(), fileId);
            }
            case EPISODE -> {
                EpisodeFile file = episodeFileRepository.findById(fileId)
                        .orElseThrow(() -> new MovieNotFoundException("Episode file not found: " + fileId));
                deleteFileFromStorage(file.getMinioObjectKey());
                episodeFileRepository.delete(file);
                log.info("Episode file deleted: {} ({})", file.getOriginalFilename(), fileId);
            }
        }
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredPresignedUrls() {
        LocalDateTime now = LocalDateTime.now();
        List<MovieFile.UploadStatus> movieStatuses = Arrays.asList(
                MovieFile.UploadStatus.PENDING, MovieFile.UploadStatus.UPLOADING);
        List<EpisodeFile.UploadStatus> episodeStatuses = Arrays.asList(
                EpisodeFile.UploadStatus.PENDING, EpisodeFile.UploadStatus.UPLOADING);

        List<MovieFile> expiredMovieFiles = movieFileRepository.findExpiredPresignedUrls(movieStatuses, now);
        List<EpisodeFile> expiredEpisodeFiles = episodeFileRepository.findExpiredPresignedUrls(episodeStatuses, now);

        for (MovieFile file : expiredMovieFiles) {
            file.setUploadStatus(MovieFile.UploadStatus.FAILED);
            file.setPresignedUrl(null);
            file.setPresignedExpiresAt(null);
            movieFileRepository.save(file);
        }

        for (EpisodeFile file : expiredEpisodeFiles) {
            file.setUploadStatus(EpisodeFile.UploadStatus.FAILED);
            file.setPresignedUrl(null);
            file.setPresignedExpiresAt(null);
            episodeFileRepository.save(file);
        }

        int totalExpired = expiredMovieFiles.size() + expiredEpisodeFiles.size();
        if (totalExpired > 0) {
            log.info("Cleaned up {} expired presigned URLs", totalExpired);
        }
    }

    private void validateUploadRequest(FileUploadRequest request) {
        switch (request.getEntityType()) {
            case MOVIE -> {
                if (!movieRepository.existsById(request.getEntityId())) {
                    throw new IllegalArgumentException("Movie not found: " + request.getEntityId());
                }

                if (movieFileRepository.existsByMovieIdAndFileType(
                        request.getEntityId(), request.getMovieFileType())) {
                    throw new IllegalArgumentException("File of type " + request.getFileType() +
                            " already exists for movie " + request.getEntityId());
                }
            }
            case EPISODE -> {
                if (!episodeRepository.existsById(request.getEntityId())) {
                    throw new IllegalArgumentException("Episode not found: " + request.getEntityId());
                }

                if (episodeFileRepository.existsByEpisodeIdAndFileType(
                        request.getEntityId(), request.getEpisodeFileType())) {
                    throw new IllegalArgumentException("File of type " + request.getFileType() +
                            " already exists for episode " + request.getEntityId());
                }
            }
        }

        validateFileConstraints(request);
    }

    private void validateFileConstraints(FileUploadRequest request) {
        switch (request.getFileType().toLowerCase()) {
            case "video" -> {
                if (!isValidVideoFile(request.getOriginalFilename(), request.getMimeType())) {
                    throw new IllegalArgumentException("Invalid video file format");
                }
                if (request.getFileSize() > 20L * 1024 * 1024 * 1024) { // 20GB
                    throw new IllegalArgumentException("Video file size exceeds maximum limit of 20GB");
                }
            }
            case "cover" -> {
                if (!isValidImageFile(request.getOriginalFilename(), request.getMimeType())) {
                    throw new IllegalArgumentException("Invalid image file format");
                }
                if (request.getFileSize() > 10L * 1024 * 1024) { // 10MB
                    throw new IllegalArgumentException("Image file size exceeds maximum limit of 10MB");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported file type: " + request.getFileType());
        }
    }

    private boolean isValidVideoFile(String filename, String mimeType) {
        if (filename == null || mimeType == null) return false;
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm");
        return allowedExtensions.contains(extension) || mimeType.startsWith("video/");
    }

    private boolean isValidImageFile(String filename, String mimeType) {
        if (filename == null || mimeType == null) return false;
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "webp");
        return allowedExtensions.contains(extension) || mimeType.startsWith("image/");
    }

    private String generateObjectKey(FileUploadRequest request) {
        String extension = "";
        if (request.getOriginalFilename() != null && request.getOriginalFilename().contains(".")) {
            extension = request.getOriginalFilename().substring(request.getOriginalFilename().lastIndexOf("."));
        }

        String basePath = switch (request.getEntityType()) {
            case MOVIE -> "movies/" + request.getEntityId() + "/";
            case EPISODE -> "episodes/" + request.getEntityId() + "/";
        };

        String fileTypePath = request.getFileType().toLowerCase() + "/";

        return basePath + fileTypePath + UUID.randomUUID() + extension;
    }

    private String generatePresignedUrl(String objectKey) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .expiry(Math.toIntExact(UPLOAD_EXPIRY_HOURS), TimeUnit.HOURS)
                        .build());
    }

    private void createMovieFileRecord(FileUploadRequest request, String uploadSessionId,
                                       String objectKey, String presignedUrl, LocalDateTime expiresAt) {
        MovieFile movieFile = MovieFile.builder()
                .movieId(request.getEntityId())
                .fileType(request.getMovieFileType())
                .originalFilename(request.getOriginalFilename())
                .minioBucket(minioProperties.getBucket())
                .minioObjectKey(objectKey)
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .uploadStatus(MovieFile.UploadStatus.PENDING)
                .uploadSessionId(uploadSessionId)
                .presignedUrl(presignedUrl)
                .presignedExpiresAt(expiresAt)
                .build();

        movieFileRepository.save(movieFile);
    }

    private void createEpisodeFileRecord(FileUploadRequest request, String uploadSessionId,
                                         String objectKey, String presignedUrl, LocalDateTime expiresAt) {
        EpisodeFile episodeFile = EpisodeFile.builder()
                .episodeId(request.getEntityId())
                .fileType(request.getEpisodeFileType())
                .originalFilename(request.getOriginalFilename())
                .minioBucket(minioProperties.getBucket())
                .minioObjectKey(objectKey)
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .uploadStatus(EpisodeFile.UploadStatus.PENDING)
                .uploadSessionId(uploadSessionId)
                .presignedUrl(presignedUrl)
                .presignedExpiresAt(expiresAt)
                .build();

        episodeFileRepository.save(episodeFile);
    }

    private void markMovieFileAsCompleted(MovieFile file) {
        file.setUploadStatus(MovieFile.UploadStatus.COMPLETED);
        file.setCompletedAt(LocalDateTime.now());
        file.setPresignedUrl(null);
        file.setPresignedExpiresAt(null);
        movieFileRepository.save(file);
        log.info("Movie file upload completed: {} for movie {}", file.getOriginalFilename(), file.getMovieId());
    }

    private void markEpisodeFileAsCompleted(EpisodeFile file) {
        file.setUploadStatus(EpisodeFile.UploadStatus.COMPLETED);
        file.setCompletedAt(LocalDateTime.now());
        file.setPresignedUrl(null);
        file.setPresignedExpiresAt(null);
        episodeFileRepository.save(file);
        log.info("Episode file upload completed: {} for episode {}", file.getOriginalFilename(), file.getEpisodeId());
    }

    private void deleteFileFromStorage(String objectKey) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build());
            log.debug("File deleted from storage: {}", objectKey);
        } catch (Exception e) {
            log.error("Error deleting file from storage {}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("Error deleting file from storage: " + objectKey, e);
        }
    }

    private FileInfoDto mapMovieFileToDto(MovieFile file) {
        return FileInfoDto.builder()
                .id(file.getId())
                .fileType(file.getFileType().name())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .uploadStatus(file.getUploadStatus().name())
                .uploadSessionId(file.getUploadSessionId())
                .createdAt(file.getCreatedAt())
                .completedAt(file.getCompletedAt())
                .downloadUrl(generateDownloadUrl(file.getMovieId(), file.getFileType(), "movie"))
                .build();
    }

    private FileInfoDto mapEpisodeFileToDto(EpisodeFile file) {
        return FileInfoDto.builder()
                .id(file.getId())
                .fileType(file.getFileType().name())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .uploadStatus(file.getUploadStatus().name())
                .uploadSessionId(file.getUploadSessionId())
                .createdAt(file.getCreatedAt())
                .completedAt(file.getCompletedAt())
                .downloadUrl(generateDownloadUrl(file.getEpisodeId(), file.getFileType(), "episode"))
                .build();
    }

    private String generateDownloadUrl(Long entityId, Enum<?> fileType, String entityType) {
        String publicUrl = "http://localhost:8081/api";
        if ("COVER".equals(fileType.name())) {
            if ("movie".equals(entityType)) {
                return publicUrl + "/stream/movies/" + entityId + "/cover";
            } else {
                return publicUrl + "/stream/episodes/" + entityId + "/cover";
            }
        }

        if ("VIDEO".equals(fileType.name())) {
            if ("movie".equals(entityType)) {
                return publicUrl + "/stream/movies/" + entityId;
            } else {
                return publicUrl + "/stream/episodes/" + entityId;
            }
        }

        return null;
    }
}
