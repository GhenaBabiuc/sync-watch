package com.example.storageservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioWebhookService {

    private final FileUploadService fileUploadService;

    @Transactional
    public void processNotification(Map<String, Object> notification) {
        try {
            List<Map<String, Object>> records = extractRecords(notification);

            for (Map<String, Object> record : records) {
                processRecord(record);
            }

        } catch (Exception e) {
            log.error("Error processing MinIO notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process notification", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRecords(Map<String, Object> notification) {
        Object recordsObj = notification.get("Records");
        if (recordsObj instanceof List) {
            return (List<Map<String, Object>>) recordsObj;
        }
        throw new IllegalArgumentException("Invalid notification format: missing Records");
    }

    @SuppressWarnings("unchecked")
    private void processRecord(Map<String, Object> record) {
        try {
            String eventName = (String) record.get("eventName");

            if (!isObjectCreatedEvent(eventName)) {
                log.debug("Ignoring non-creation event: {}", eventName);
                return;
            }

            Map<String, Object> s3 = (Map<String, Object>) record.get("s3");
            if (s3 == null) {
                log.warn("Missing s3 data in record: {}", record);
                return;
            }

            Map<String, Object> bucket = (Map<String, Object>) s3.get("bucket");
            Map<String, Object> object = (Map<String, Object>) s3.get("object");

            if (bucket == null || object == null) {
                log.warn("Missing bucket or object data in record: {}", record);
                return;
            }

            String bucketName = (String) bucket.get("name");
            String objectKey = (String) object.get("key");

            if (bucketName == null || objectKey == null) {
                log.warn("Missing bucket name or object key in record: {}", record);
                return;
            }

            String decodedObjectKey = URLDecoder.decode(objectKey, StandardCharsets.UTF_8);
            log.info("Processing object creation for bucket: {}, key: {}", bucketName, decodedObjectKey);

            fileUploadService.handleFileUploadCompletion(bucketName, decodedObjectKey);

        } catch (Exception e) {
            log.error("Error processing record {}: {}", record, e.getMessage(), e);
        }
    }

    private boolean isObjectCreatedEvent(String eventName) {
        return eventName != null && (
                eventName.equals("s3:ObjectCreated:Put") ||
                        eventName.equals("s3:ObjectCreated:Post") ||
                        eventName.equals("s3:ObjectCreated:CompleteMultipartUpload")
        );
    }
}
