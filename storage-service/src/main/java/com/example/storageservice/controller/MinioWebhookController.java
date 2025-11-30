package com.example.storageservice.controller;

import com.example.storageservice.service.MinioWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/minio")
@RequiredArgsConstructor
public class MinioWebhookController {

    private final MinioWebhookService minioWebhookService;

    @PostMapping("/notification")
    public ResponseEntity<Map<String, String>> handleMinioNotification(@RequestBody Map<String, Object> notification) {
        try {
            minioWebhookService.processNotification(notification);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Notification processed"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
