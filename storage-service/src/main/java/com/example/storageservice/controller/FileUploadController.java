package com.example.storageservice.controller;

import com.example.storageservice.model.dto.FileUploadRequest;
import com.example.storageservice.model.dto.FileUploadResponse;
import com.example.storageservice.service.FileUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> initiateFileUpload(@Valid @RequestBody FileUploadRequest request) {
        FileUploadResponse response = fileUploadService.initiateFileUpload(request);

        return ResponseEntity.ok(response);
    }
}
