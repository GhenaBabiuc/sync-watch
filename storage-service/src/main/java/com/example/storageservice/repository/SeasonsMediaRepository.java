package com.example.storageservice.repository;

import com.example.storageservice.model.MediaCategory;
import com.example.storageservice.model.SeasonMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonsMediaRepository extends JpaRepository<SeasonMedia, Long> {
    List<SeasonMedia> findBySeasonId(Long seasonId);

    Optional<SeasonMedia> findBySeasonIdAndCategory(Long seasonId, MediaCategory category);

    Optional<SeasonMedia> findBySeasonIdAndCategoryAndIsPrimaryTrue(Long seasonId, MediaCategory category);
}
