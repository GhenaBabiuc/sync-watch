package com.example.storageservice.repository;

import com.example.storageservice.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query("SELECT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:year IS NULL OR m.year = :year) " +
            "ORDER BY m.createdAt DESC")
    Page<Movie> findMoviesWithFilters(@Param("title") String title,
                                      @Param("year") Integer year,
                                      Pageable pageable);
}
