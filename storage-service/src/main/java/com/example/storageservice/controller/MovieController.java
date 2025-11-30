package com.example.storageservice.controller;

import com.example.storageservice.model.dto.CreateMovieRequest;
import com.example.storageservice.model.dto.MovieDto;
import com.example.storageservice.model.dto.UpdateMovieRequest;
import com.example.storageservice.service.MovieService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<Page<MovieDto>> getAllMovies(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year) {
        Page<MovieDto> movies = movieService.getAllMovies(page, size, title, year);

        return ResponseEntity.ok(movies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDto> getMovieById(@PathVariable Long id) {
        MovieDto movie = movieService.getMovieById(id);

        return ResponseEntity.ok(movie);
    }

    @PostMapping
    public ResponseEntity<MovieDto> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        log.info("Creating movie: {}", request.getTitle());
        MovieDto movie = movieService.createMovie(request);

        return ResponseEntity.ok(movie);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieDto> updateMovie(@PathVariable Long id, @Valid @RequestBody UpdateMovieRequest request) {
        log.info("Updating movie ID: {}", id);
        MovieDto updated = movieService.updateMovie(id, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        log.info("Deleting movie ID: {}", id);
        movieService.deleteMovie(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MovieDto>> searchMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Searching movies with filters - title: {}, year: {}, minDuration: {}, maxDuration: {}", title, year, minDuration, maxDuration);
        Page<MovieDto> movies = movieService.searchMovies(title, year, minDuration, maxDuration, page, size);

        return ResponseEntity.ok(movies);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getMoviesCount() {
        log.info("Getting total movies count");
        long count = movieService.getMoviesCount();

        return ResponseEntity.ok(count);
    }
}
