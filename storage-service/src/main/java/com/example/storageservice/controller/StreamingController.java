package com.example.storageservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.storageservice.service.StreamingService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Tag(name = "Streaming Controller", description = "Video streaming endpoints with Range support")
@CrossOrigin(origins = "*")
public class StreamingController {

    private final StreamingService streamingService;

    @Operation(
            summary = "Stream movie video",
            description = "Streams movie video file with HTTP Range support for seeking and adaptive playback. " +
                    "Supports partial content requests (206) for efficient video streaming."
    )
    @GetMapping("/movies/{movieId}")
    public ResponseEntity<InputStreamResource> streamMovie(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long movieId,
            HttpServletRequest request) {

        String clientIp = getClientIpAddress(request);
        String rangeHeader = request.getHeader("Range");

        log.info("Streaming movie ID: {} from IP: {}, Range: {}", movieId, clientIp, rangeHeader);

        return streamingService.streamMovie(movieId, request);
    }

    @Operation(
            summary = "Stream episode video",
            description = "Streams TV series episode video file with HTTP Range support for seeking and adaptive playback. " +
                    "Supports partial content requests (206) for efficient video streaming."
    )
    @GetMapping("/episodes/{episodeId}")
    public ResponseEntity<InputStreamResource> streamEpisode(
            @Parameter(description = "Episode ID", required = true) @PathVariable Long episodeId,
            HttpServletRequest request) {

        String clientIp = getClientIpAddress(request);
        String rangeHeader = request.getHeader("Range");

        log.info("Streaming episode ID: {} from IP: {}, Range: {}", episodeId, clientIp, rangeHeader);

        return streamingService.streamEpisode(episodeId, request);
    }

    @Operation(
            summary = "Get movie cover image",
            description = "Returns cover image for a movie"
    )
    @GetMapping("/movies/{movieId}/cover")
    public ResponseEntity<InputStreamResource> getMovieCover(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long movieId) {

        log.info("Getting cover for movie ID: {}", movieId);

        return streamingService.getMovieCover(movieId);
    }

    @Operation(
            summary = "Get episode cover image",
            description = "Returns cover image for an episode"
    )
    @GetMapping("/episodes/{episodeId}/cover")
    public ResponseEntity<InputStreamResource> getEpisodeCover(
            @Parameter(description = "Episode ID", required = true) @PathVariable Long episodeId) {

        log.info("Getting cover for episode ID: {}", episodeId);

        return streamingService.getEpisodeCover(episodeId);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
