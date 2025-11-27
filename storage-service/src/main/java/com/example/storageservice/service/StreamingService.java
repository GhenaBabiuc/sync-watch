package com.example.storageservice.service;

import com.example.storageservice.model.EpisodeFile;
import com.example.storageservice.model.MovieFile;
import com.example.storageservice.repository.EpisodeFileRepository;
import com.example.storageservice.repository.MovieFileRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final MinioClient minioClient;
    private final MovieFileRepository movieFileRepository;
    private final EpisodeFileRepository episodeFileRepository;

    public ResponseEntity<InputStreamResource> streamMovie(Long movieId, HttpServletRequest request) {
        try {
            Optional<MovieFile> videoFileOpt = movieFileRepository
                    .findByMovieIdAndFileType(movieId, MovieFile.FileType.VIDEO)
                    .filter(file -> MovieFile.UploadStatus.COMPLETED.equals(file.getUploadStatus()));

            if (videoFileOpt.isEmpty()) {
                log.warn("Video file not found or not completed for movie ID: {}", movieId);
                return ResponseEntity.notFound().build();
            }

            MovieFile videoFile = videoFileOpt.get();
            return handleVideoStream(videoFile.getMinioBucket(), videoFile.getMinioObjectKey(),
                    videoFile.getFileSize(), videoFile.getMimeType(), request);

        } catch (Exception e) {
            log.error("Error streaming movie ID {}: {}", movieId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<InputStreamResource> streamEpisode(Long episodeId, HttpServletRequest request) {
        try {
            Optional<EpisodeFile> videoFileOpt = episodeFileRepository
                    .findByEpisodeIdAndFileType(episodeId, EpisodeFile.FileType.VIDEO)
                    .filter(file -> EpisodeFile.UploadStatus.COMPLETED.equals(file.getUploadStatus()));

            if (videoFileOpt.isEmpty()) {
                log.warn("Video file not found or not completed for episode ID: {}", episodeId);
                return ResponseEntity.notFound().build();
            }

            EpisodeFile videoFile = videoFileOpt.get();
            return handleVideoStream(videoFile.getMinioBucket(), videoFile.getMinioObjectKey(),
                    videoFile.getFileSize(), videoFile.getMimeType(), request);

        } catch (Exception e) {
            log.error("Error streaming episode ID {}: {}", episodeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<InputStreamResource> getMovieCover(Long movieId) {
        try {
            log.debug("Fetching cover for movie ID: {}", movieId);

            Optional<MovieFile> coverFileOpt = movieFileRepository
                    .findByMovieIdAndFileType(movieId, MovieFile.FileType.COVER)
                    .filter(file -> MovieFile.UploadStatus.COMPLETED.equals(file.getUploadStatus()));

            if (coverFileOpt.isEmpty()) {
                log.warn("Cover file not found or not completed for movie ID: {}", movieId);
                return ResponseEntity.notFound().build();
            }

            MovieFile coverFile = coverFileOpt.get();
            return streamImageFile(coverFile.getMinioBucket(), coverFile.getMinioObjectKey(),
                    coverFile.getMimeType());

        } catch (Exception e) {
            log.error("Error getting cover for movie ID {}: {}", movieId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<InputStreamResource> getEpisodeCover(Long episodeId) {
        try {
            log.debug("Fetching cover for episode ID: {}", episodeId);

            Optional<EpisodeFile> coverFileOpt = episodeFileRepository
                    .findByEpisodeIdAndFileType(episodeId, EpisodeFile.FileType.COVER)
                    .filter(file -> EpisodeFile.UploadStatus.COMPLETED.equals(file.getUploadStatus()));

            if (coverFileOpt.isEmpty()) {
                log.warn("Cover file not found or not completed for episode ID: {}", episodeId);
                return ResponseEntity.notFound().build();
            }

            EpisodeFile coverFile = coverFileOpt.get();
            return streamImageFile(coverFile.getMinioBucket(), coverFile.getMinioObjectKey(),
                    coverFile.getMimeType());

        } catch (Exception e) {
            log.error("Error getting cover for episode ID {}: {}", episodeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<InputStreamResource> streamImageFile(String bucket, String objectKey, String mimeType) {
        try {
            log.info("Streaming image from bucket: {}, key: {}", bucket, objectKey);

            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );

            String contentType = mimeType;
            if (contentType == null || contentType.isEmpty()) {
                contentType = determineImageMimeType(objectKey);
            }

            log.info("Successfully streaming image: {} (type: {})", objectKey, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            log.error("Error streaming image {}: {}", objectKey, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineImageMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private ResponseEntity<InputStreamResource> handleVideoStream(String bucket, String objectKey,
                                                                  Long fileSize, String mimeType, HttpServletRequest request) {

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(bucket, objectKey, fileSize, mimeType, rangeHeader);
        } else {
            return handleFullFileRequest(bucket, objectKey, fileSize, mimeType);
        }
    }

    private ResponseEntity<InputStreamResource> handleRangeRequest(String bucket, String objectKey,
                                                                   Long fileSize, String mimeType, String rangeHeader) {
        try {
            Pattern pattern = Pattern.compile("bytes=(\\d+)-(\\d*)");
            Matcher matcher = pattern.matcher(rangeHeader);

            if (!matcher.find()) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }

            long start = Long.parseLong(matcher.group(1));
            long end = matcher.group(2).isEmpty() ? fileSize - 1 : Long.parseLong(matcher.group(2));

            if (start >= fileSize || end >= fileSize || start > end) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }

            long contentLength = end - start + 1;

            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .offset(start)
                            .length(contentLength)
                            .build()
            );

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileSize))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            log.error("Error handling range request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<InputStreamResource> handleFullFileRequest(String bucket, String objectKey,
                                                                      Long fileSize, String mimeType) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            log.error("Error handling full file request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
