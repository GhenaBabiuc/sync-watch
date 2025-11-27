package com.example.storageservice.repository;

import com.example.storageservice.model.EpisodeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeFileRepository extends JpaRepository<EpisodeFile, Long> {

    List<EpisodeFile> findByEpisodeId(Long episodeId);

    Optional<EpisodeFile> findByEpisodeIdAndFileType(Long episodeId, EpisodeFile.FileType fileType);

    List<EpisodeFile> findByUploadStatus(EpisodeFile.UploadStatus status);

    Optional<EpisodeFile> findByUploadSessionId(String uploadSessionId);

    @Query("SELECT ef FROM EpisodeFile ef WHERE ef.uploadStatus IN :statuses AND ef.presignedExpiresAt < :dateTime")
    List<EpisodeFile> findExpiredPresignedUrls(@Param("statuses") List<EpisodeFile.UploadStatus> statuses,
                                               @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT ef FROM EpisodeFile ef WHERE ef.minioBucket = :bucket AND ef.minioObjectKey = :objectKey")
    Optional<EpisodeFile> findByMinioLocation(@Param("bucket") String bucket, @Param("objectKey") String objectKey);

    boolean existsByEpisodeIdAndFileType(Long episodeId, EpisodeFile.FileType fileType);

    @Query("SELECT ef FROM EpisodeFile ef JOIN ef.episode e JOIN e.season s WHERE s.seriesId = :seriesId")
    List<EpisodeFile> findBySeriesId(@Param("seriesId") Long seriesId);

    @Query("SELECT ef FROM EpisodeFile ef JOIN ef.episode e WHERE e.seasonId = :seasonId")
    List<EpisodeFile> findBySeasonId(@Param("seasonId") Long seasonId);
}
