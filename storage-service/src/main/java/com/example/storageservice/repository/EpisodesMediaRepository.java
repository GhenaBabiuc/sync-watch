package com.example.storageservice.repository;

import com.example.storageservice.model.EpisodeMedia;
import com.example.storageservice.model.MediaCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodesMediaRepository extends JpaRepository<EpisodeMedia, Long> {
    List<EpisodeMedia> findByEpisodeId(Long episodeId);

    Optional<EpisodeMedia> findByEpisodeIdAndCategory(Long episodeId, MediaCategory category);

    Optional<EpisodeMedia> findByEpisodeIdAndCategoryAndIsPrimaryTrue(Long episodeId, MediaCategory category);
}
