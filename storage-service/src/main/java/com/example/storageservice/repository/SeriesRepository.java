package com.example.storageservice.repository;

import com.example.storageservice.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long>, QuerydslPredicateExecutor<Series> {

}
