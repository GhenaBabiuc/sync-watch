package com.example.storageservice.repository;

import com.example.storageservice.model.MediaCategory;
import com.example.storageservice.model.SeriesMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesMediaRepository extends JpaRepository<SeriesMedia, Long> {
    List<SeriesMedia> findBySeriesId(Long seriesId);

    Optional<SeriesMedia> findBySeriesIdAndCategory(Long seriesId, MediaCategory category);

    Optional<SeriesMedia> findBySeriesIdAndCategoryAndIsPrimaryTrue(Long seriesId, MediaCategory category);
}
