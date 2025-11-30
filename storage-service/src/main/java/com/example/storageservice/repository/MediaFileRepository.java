package com.example.storageservice.repository;

import com.example.storageservice.model.MediaFile;
import com.example.storageservice.model.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    @Query("SELECT m FROM MediaFile m WHERE m.minioBucket = :bucket AND m.minioObjectKey = :key")
    Optional<MediaFile> findByMinioLocation(@Param("bucket") String bucket, @Param("key") String key);

    @Query("SELECT m FROM MediaFile m WHERE m.uploadStatus IN :statuses AND m.presignedExpiresAt < :now")
    List<MediaFile> findExpiredPresignedUrls(@Param("statuses") List<UploadStatus> statuses, @Param("now") LocalDateTime now);
}
