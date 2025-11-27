package com.example.storageservice.repository;

import com.example.storageservice.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {

    List<Season> findBySeriesIdOrderBySeasonNumber(Long seriesId);

    @Query("SELECT s FROM Season s WHERE s.seriesId = :seriesId AND s.seasonNumber = :seasonNumber")
    Optional<Season> findBySeriesIdAndSeasonNumber(@Param("seriesId") Long seriesId,
                                                   @Param("seasonNumber") Integer seasonNumber);

    boolean existsBySeriesIdAndSeasonNumber(Long seriesId, Integer seasonNumber);
}
