package com.example.storageservice.service;

import com.example.storageservice.config.MinioProperties;
import com.example.storageservice.model.Episode;
import com.example.storageservice.model.EpisodeMedia;
import com.example.storageservice.model.MediaFile;
import com.example.storageservice.model.Movie;
import com.example.storageservice.model.MovieMedia;
import com.example.storageservice.model.Season;
import com.example.storageservice.model.SeasonMedia;
import com.example.storageservice.model.Series;
import com.example.storageservice.model.SeriesMedia;
import com.example.storageservice.model.UploadStatus;
import com.example.storageservice.model.dto.FileUploadRequest;
import com.example.storageservice.model.dto.FileUploadResponse;
import com.example.storageservice.repository.EpisodeRepository;
import com.example.storageservice.repository.EpisodesMediaRepository;
import com.example.storageservice.repository.MediaFileRepository;
import com.example.storageservice.repository.MovieRepository;
import com.example.storageservice.repository.MoviesMediaRepository;
import com.example.storageservice.repository.SeasonRepository;
import com.example.storageservice.repository.SeasonsMediaRepository;
import com.example.storageservice.repository.SeriesMediaRepository;
import com.example.storageservice.repository.SeriesRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final MediaFileRepository mediaFileRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;

    private final MoviesMediaRepository moviesMediaRepository;
    private final EpisodesMediaRepository episodesMediaRepository;
    private final SeriesMediaRepository seriesMediaRepository;
    private final SeasonsMediaRepository seasonsMediaRepository;

    private static final long UPLOAD_EXPIRY_HOURS = 24;

    @Transactional
    public FileUploadResponse initiateFileUpload(FileUploadRequest request) {
        validateEntityExists(request);

        String uploadSessionId = UUID.randomUUID().toString();
        String objectKey = generateObjectKey(request);

        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .expiry(Math.toIntExact(UPLOAD_EXPIRY_HOURS), TimeUnit.HOURS)
                            .build());

            LocalDateTime expiresAt = LocalDateTime.now().plusHours(UPLOAD_EXPIRY_HOURS);

            MediaFile mediaFile = MediaFile.builder()
                    .originalFilename(request.getOriginalFilename())
                    .contentType(request.getMimeType())
                    .fileSize(request.getFileSize())
                    .minioBucket(minioProperties.getBucket())
                    .minioObjectKey(objectKey)
                    .uploadStatus(UploadStatus.PENDING)
                    .uploadSessionId(uploadSessionId)
                    .presignedUrl(presignedUrl)
                    .presignedExpiresAt(expiresAt)
                    .build();

            mediaFile = mediaFileRepository.save(mediaFile);

            linkMediaToEntity(mediaFile, request);

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
            log.error("Error initiating file upload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initiate file upload", e);
        }
    }

    @Async
    @Transactional
    public void handleFileUploadCompletion(String bucket, String objectKey) {
        log.info("Processing file upload completion for object: {}", objectKey);

        mediaFileRepository.findByMinioLocation(bucket, objectKey)
                .ifPresentOrElse(file -> {
                    file.setUploadStatus(UploadStatus.COMPLETED);
                    file.setPresignedUrl(null);
                    file.setPresignedExpiresAt(null);
                    mediaFileRepository.save(file);
                    log.info("Media file upload completed: {} (ID: {})", file.getOriginalFilename(), file.getId());
                }, () -> log.warn("No media file record found for key: {}", objectKey));
    }

    @Transactional
    public void deleteFile(Long mediaFileId) {
        MediaFile file = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new IllegalArgumentException("Media file not found: " + mediaFileId));

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(file.getMinioBucket())
                            .object(file.getMinioObjectKey())
                            .build());
            log.debug("File deleted from storage: {}", file.getMinioObjectKey());
        } catch (Exception e) {
            log.error("Error deleting file from storage: {}", e.getMessage());
        }

        mediaFileRepository.delete(file);
        log.info("Media file record deleted: {}", mediaFileId);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredPresignedUrls() {
        LocalDateTime now = LocalDateTime.now();
        List<UploadStatus> statuses = Arrays.asList(UploadStatus.PENDING, UploadStatus.UPLOADING);

        List<MediaFile> expiredFiles = mediaFileRepository.findExpiredPresignedUrls(statuses, now);

        for (MediaFile file : expiredFiles) {
            file.setUploadStatus(UploadStatus.FAILED);
            file.setPresignedUrl(null);
            file.setPresignedExpiresAt(null);
            mediaFileRepository.save(file);
        }

        if (!expiredFiles.isEmpty()) {
            log.info("Cleaned up {} expired media files", expiredFiles.size());
        }
    }

    private void validateEntityExists(FileUploadRequest request) {
        boolean exists = switch (request.getEntityType()) {
            case MOVIE -> movieRepository.existsById(request.getEntityId());
            case EPISODE -> episodeRepository.existsById(request.getEntityId());
            case SERIES -> seriesRepository.existsById(request.getEntityId());
            case SEASON -> seasonRepository.existsById(request.getEntityId());
        };

        if (!exists) {
            throw new IllegalArgumentException(request.getEntityType() + " not found: " + request.getEntityId());
        }
    }

    private void linkMediaToEntity(MediaFile mediaFile, FileUploadRequest request) {
        switch (request.getEntityType()) {
            case MOVIE -> {
                Movie movie = movieRepository.getReferenceById(request.getEntityId());
                MovieMedia link = MovieMedia.builder()
                        .movie(movie)
                        .mediaFile(mediaFile)
                        .category(request.getCategory())
                        .isPrimary(request.isPrimary())
                        .build();
                moviesMediaRepository.save(link);
            }
            case EPISODE -> {
                Episode episode = episodeRepository.getReferenceById(request.getEntityId());
                EpisodeMedia link = EpisodeMedia.builder()
                        .episode(episode)
                        .mediaFile(mediaFile)
                        .category(request.getCategory())
                        .isPrimary(request.isPrimary())
                        .build();
                episodesMediaRepository.save(link);
            }
            case SERIES -> {
                Series series = seriesRepository.getReferenceById(request.getEntityId());
                SeriesMedia link = SeriesMedia.builder()
                        .series(series)
                        .mediaFile(mediaFile)
                        .category(request.getCategory())
                        .isPrimary(request.isPrimary())
                        .build();
                seriesMediaRepository.save(link);
            }
            case SEASON -> {
                Season season = seasonRepository.getReferenceById(request.getEntityId());
                SeasonMedia link = SeasonMedia.builder()
                        .season(season)
                        .mediaFile(mediaFile)
                        .category(request.getCategory())
                        .isPrimary(request.isPrimary())
                        .build();
                seasonsMediaRepository.save(link);
            }
        }
    }

    private String generateObjectKey(FileUploadRequest request) {
        String extension = "";
        if (request.getOriginalFilename() != null && request.getOriginalFilename().contains(".")) {
            extension = request.getOriginalFilename().substring(request.getOriginalFilename().lastIndexOf("."));
        }

        return String.format("%ss/%d/%s/%s%s",
                request.getEntityType().name().toLowerCase(),
                request.getEntityId(),
                request.getCategory().name().toLowerCase(),
                UUID.randomUUID(),
                extension);
    }
}
