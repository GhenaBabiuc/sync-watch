package com.example.storageservice.repository;

import com.example.storageservice.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, QuerydslPredicateExecutor<Movie> {

}
