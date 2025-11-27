package com.example.storageservice.repository;

import com.example.storageservice.model.MovieFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieFileRepository extends JpaRepository<MovieFile, Long> {

    List<MovieFile> findByMovieId(Long movieId);

    Optional<MovieFile> findByMovieIdAndFileType(Long movieId, MovieFile.FileType fileType);

    List<MovieFile> findByUploadStatus(MovieFile.UploadStatus status);

    Optional<MovieFile> findByUploadSessionId(String uploadSessionId);

    @Query("SELECT mf FROM MovieFile mf WHERE mf.uploadStatus IN :statuses AND mf.presignedExpiresAt < :dateTime")
    List<MovieFile> findExpiredPresignedUrls(@Param("statuses") List<MovieFile.UploadStatus> statuses,
                                             @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT mf FROM MovieFile mf WHERE mf.minioBucket = :bucket AND mf.minioObjectKey = :objectKey")
    Optional<MovieFile> findByMinioLocation(@Param("bucket") String bucket, @Param("objectKey") String objectKey);

    boolean existsByMovieIdAndFileType(Long movieId, MovieFile.FileType fileType);
}
