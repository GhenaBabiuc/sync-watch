package com.example.storageservice.repository;

import com.example.storageservice.model.Series;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {

    @Query("SELECT s FROM Series s WHERE " +
            "(:year IS NULL OR s.year = :year) " +
            "ORDER BY s.createdAt DESC")
    Page<Series> findSeriesWithFilters(@Param("title") String title,
                                       @Param("year") Integer year,
                                       Pageable pageable);
}
