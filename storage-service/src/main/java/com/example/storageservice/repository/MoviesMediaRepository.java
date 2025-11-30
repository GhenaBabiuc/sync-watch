package com.example.storageservice.repository;

import com.example.storageservice.model.MediaCategory;
import com.example.storageservice.model.MovieMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoviesMediaRepository extends JpaRepository<MovieMedia, Long> {
    List<MovieMedia> findByMovieId(Long movieId);

    Optional<MovieMedia> findByMovieIdAndCategory(Long movieId, MediaCategory category);

    Optional<MovieMedia> findByMovieIdAndCategoryAndIsPrimaryTrue(Long movieId, MediaCategory category);
}
