package com.example.storageservice.controller;

import com.example.storageservice.service.MinioWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/minio")
@RequiredArgsConstructor
@Tag(name = "MinIO Webhook Controller", description = "Handles MinIO bucket notifications")
public class MinioWebhookController {

    private final MinioWebhookService minioWebhookService;

    @Operation(
            summary = "Handle MinIO bucket notification",
            description = "Processes MinIO bucket events like object creation completion"
    )
    @PostMapping("/notification")
    public ResponseEntity<Map<String, String>> handleMinioNotification(@RequestBody Map<String, Object> notification) {
        log.info("Received MinIO notification: {}", notification);

        try {
            minioWebhookService.processNotification(notification);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Notification processed"));
        } catch (Exception e) {
            log.error("Error processing MinIO notification: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Health check for webhook endpoint",
            description = "Simple health check for MinIO webhook configuration"
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("MinIO webhook health check requested");
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "minio-webhook",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        log.info("Test webhook called with payload: {}", payload);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test webhook received",
                "payload", payload != null ? payload.toString() : "empty"
        ));
    }
}
