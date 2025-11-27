package com.example.storageservice.repository;

import com.example.storageservice.model.Episode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    List<Episode> findBySeasonIdOrderByEpisodeNumber(Long seasonId);

    @Query("SELECT e FROM Episode e WHERE e.seasonId = :seasonId AND e.episodeNumber = :episodeNumber")
    Optional<Episode> findBySeasonIdAndEpisodeNumber(@Param("seasonId") Long seasonId,
                                                     @Param("episodeNumber") Integer episodeNumber);

    boolean existsBySeasonIdAndEpisodeNumber(Long seasonId, Integer episodeNumber);

    @Query("SELECT e FROM Episode e JOIN e.season s WHERE s.seriesId = :seriesId")
    List<Episode> findBySeriesId(@Param("seriesId") Long seriesId);

    @Query("SELECT e FROM Episode e JOIN e.season s WHERE s.seriesId = :seriesId")
    Page<Episode> findBySeriesId(@Param("seriesId") Long seriesId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Episode e JOIN e.season s WHERE s.seriesId = :seriesId")
    Integer countBySeriesId(@Param("seriesId") Long seriesId);

    @Query("SELECT COUNT(e) FROM Episode e WHERE e.seasonId = :seasonId")
    Integer countBySeasonId(@Param("seasonId") Long seasonId);
}
