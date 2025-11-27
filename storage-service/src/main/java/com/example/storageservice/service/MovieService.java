package com.example.storageservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.storageservice.exception.MovieNotFoundException;
import com.example.storageservice.model.Movie;
import com.example.storageservice.model.dto.CreateMovieRequest;
import com.example.storageservice.model.dto.FileInfoDto;
import com.example.storageservice.model.dto.MovieDto;
import com.example.storageservice.model.dto.UpdateMovieRequest;
import com.example.storageservice.repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final FileUploadService fileUploadService;

    public Page<MovieDto> getAllMovies(int page, int size, String title, Integer year) {
        log.debug("Getting all movies - page: {}, size: {}, title filter: {}, year filter: {}",
                page, size, title, year);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Movie> movies = movieRepository.findMoviesWithFilters(title, year, pageable);

        return movies.map(this::mapToDto);
    }

    public MovieDto getMovieById(Long id) {
        log.debug("Getting movie by ID: {}", id);
        Movie movie = findMovieById(id);
        return mapToDto(movie);
    }

    @Transactional
    public MovieDto createMovie(CreateMovieRequest request) {
        log.info("Creating new movie: {}", request.getTitle());

        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .year(request.getYear())
                .duration(request.getDuration())
                .build();

        Movie saved = movieRepository.save(movie);
        log.info("Movie created successfully with ID: {}", saved.getId());

        return mapToDto(saved);
    }

    @Transactional
    public MovieDto updateMovie(Long id, UpdateMovieRequest request) {
        log.info("Updating movie ID: {}", id);

        Movie movie = findMovieById(id);

        if (request.getTitle() != null) {
            movie.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            movie.setDescription(request.getDescription());
        }
        if (request.getYear() != null) {
            movie.setYear(request.getYear());
        }
        if (request.getDuration() != null) {
            movie.setDuration(request.getDuration());
        }

        Movie updated = movieRepository.save(movie);
        log.info("Movie updated successfully: {}", updated.getTitle());

        return mapToDto(updated);
    }

    @Transactional
    public void deleteMovie(Long id) {
        log.info("Deleting movie ID: {}", id);

        Movie movie = findMovieById(id);

        List<FileInfoDto> files = fileUploadService.getFilesByMovie(id);
        for (FileInfoDto file : files) {
            try {
                log.debug("Deleting file: {} (ID: {})", file.getOriginalFilename(), file.getId());
            } catch (Exception e) {
                log.error("Error deleting file {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }

        movieRepository.delete(movie);
        log.info("Movie deleted successfully: {}", movie.getTitle());
    }

    public Page<MovieDto> searchMovies(String title, Integer year, Integer minDuration,
                                       Integer maxDuration, int page, int size) {
        log.debug("Searching movies - title: {}, year: {}, minDuration: {}, maxDuration: {}",
                title, year, minDuration, maxDuration);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Movie> movies = movieRepository.findMoviesWithFilters(title, year, pageable);

        if (minDuration != null || maxDuration != null) {
            movies = movies.map(movie -> {
                if (minDuration != null && movie.getDuration() != null && movie.getDuration() < minDuration) {
                    return null;
                }
                if (maxDuration != null && movie.getDuration() != null && movie.getDuration() > maxDuration) {
                    return null;
                }
                return movie;
            });
        }

        return movies.map(this::mapToDto);
    }

    public long getMoviesCount() {
        log.debug("Getting total movies count");
        return movieRepository.count();
    }

    private Movie findMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + id));
    }

    private MovieDto mapToDto(Movie movie) {
        List<FileInfoDto> files = fileUploadService.getFilesByMovie(movie.getId());

        return MovieDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .year(movie.getYear())
                .duration(movie.getDuration())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .files(files)
                .build();
    }
}
