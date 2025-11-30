package com.example.storageservice.service;

import com.example.storageservice.model.EpisodeMedia;
import com.example.storageservice.model.MediaCategory;
import com.example.storageservice.model.MediaFile;
import com.example.storageservice.model.MovieMedia;
import com.example.storageservice.model.UploadStatus;
import com.example.storageservice.repository.EpisodesMediaRepository;
import com.example.storageservice.repository.MoviesMediaRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final MinioClient minioClient;
    private final MoviesMediaRepository moviesMediaRepository;
    private final EpisodesMediaRepository episodesMediaRepository;

    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d+)-(\\d*)");

    @SneakyThrows
    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> streamMovie(Long movieId, HttpServletRequest request) {
        Optional<MediaFile> fileOpt = moviesMediaRepository.findByMovieIdAndCategory(movieId, MediaCategory.VIDEO)
                .map(MovieMedia::getMediaFile);

        return streamMediaFile(fileOpt, request);
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> streamEpisode(Long episodeId, HttpServletRequest request) {
        Optional<MediaFile> fileOpt = episodesMediaRepository.findByEpisodeIdAndCategory(episodeId, MediaCategory.VIDEO)
                .map(EpisodeMedia::getMediaFile);

        return streamMediaFile(fileOpt, request);
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> getMovieCover(Long movieId) {
        Optional<MediaFile> fileOpt = moviesMediaRepository.findByMovieIdAndCategoryAndIsPrimaryTrue(movieId, MediaCategory.POSTER)
                .or(() -> moviesMediaRepository.findByMovieIdAndCategory(movieId, MediaCategory.POSTER))
                .map(MovieMedia::getMediaFile);

        return serveStaticFile(fileOpt);
    }

    @SneakyThrows
    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> getEpisodeCover(Long episodeId) {
        Optional<MediaFile> fileOpt = episodesMediaRepository.findByEpisodeIdAndCategoryAndIsPrimaryTrue(episodeId, MediaCategory.POSTER)
                .or(() -> episodesMediaRepository.findByEpisodeIdAndCategory(episodeId, MediaCategory.POSTER))
                .map(EpisodeMedia::getMediaFile);

        return serveStaticFile(fileOpt);
    }

    private ResponseEntity<InputStreamResource> streamMediaFile(Optional<MediaFile> fileOpt, HttpServletRequest request) throws Exception {
        if (isNotPlayable(fileOpt)) {
            return ResponseEntity.notFound().build();
        }

        MediaFile file = fileOpt.get();
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(file, rangeHeader);
        } else {
            return handleFullFileRequest(file);
        }
    }

    private ResponseEntity<InputStreamResource> serveStaticFile(Optional<MediaFile> fileOpt) throws Exception {
        if (isNotPlayable(fileOpt)) {
            return ResponseEntity.notFound().build();
        }

        MediaFile file = fileOpt.get();
        InputStream inputStream = getMinioObject(file.getMinioBucket(), file.getMinioObjectKey());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType() != null ? file.getContentType() : "image/jpeg"))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400") // Кэш на сутки
                .body(new InputStreamResource(inputStream));
    }

    private boolean isNotPlayable(Optional<MediaFile> fileOpt) {
        return fileOpt.isEmpty() || !UploadStatus.COMPLETED.equals(fileOpt.get().getUploadStatus());
    }

    private ResponseEntity<InputStreamResource> handleRangeRequest(MediaFile file, String rangeHeader) throws Exception {
        Matcher matcher = RANGE_PATTERN.matcher(rangeHeader);

        if (!matcher.find()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + file.getFileSize())
                    .build();
        }

        long fileSize = file.getFileSize();
        long start = Long.parseLong(matcher.group(1));
        long end = matcher.group(2).isEmpty() ? fileSize - 1 : Long.parseLong(matcher.group(2));

        if (start >= fileSize || end >= fileSize || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        long contentLength = end - start + 1;

        InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(file.getMinioBucket())
                        .object(file.getMinioObjectKey())
                        .offset(start)
                        .length(contentLength)
                        .build()
        );

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileSize))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new InputStreamResource(inputStream));
    }

    private ResponseEntity<InputStreamResource> handleFullFileRequest(MediaFile file) throws Exception {
        InputStream inputStream = getMinioObject(file.getMinioBucket(), file.getMinioObjectKey());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getFileSize()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new InputStreamResource(inputStream));
    }

    private InputStream getMinioObject(String bucket, String key) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
    }
}
