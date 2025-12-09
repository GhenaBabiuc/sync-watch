package com.example.storageservice.service;

import com.example.storageservice.exception.MovieNotFoundException;
import com.example.storageservice.model.Movie;
import com.example.storageservice.model.QMovie;
import com.example.storageservice.model.dto.CreateMovieRequest;
import com.example.storageservice.model.dto.MovieDto;
import com.example.storageservice.model.dto.UpdateMovieRequest;
import com.example.storageservice.repository.MovieRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final FileUploadService fileUploadService;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public Page<MovieDto> getAllMovies(int page, int size, String title, Integer year) {
        return searchMovies(title, year, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public MovieDto getMovieById(Long id) {
        log.debug("Getting movie by ID: {}", id);
        Movie movie = findMovieById(id);
        return modelMapper.map(movie, MovieDto.class);
    }

    @Transactional
    public MovieDto createMovie(CreateMovieRequest request) {
        log.info("Creating new movie: {}", request.getTitle());

        Movie movie = modelMapper.map(request, Movie.class);
        Movie saved = movieRepository.save(movie);

        log.info("Movie created successfully with ID: {}", saved.getId());
        return modelMapper.map(saved, MovieDto.class);
    }

    @Transactional
    public MovieDto updateMovie(Long id, UpdateMovieRequest request) {
        log.info("Updating movie ID: {}", id);

        Movie movie = findMovieById(id);
        modelMapper.map(request, movie);

        Movie updated = movieRepository.save(movie);
        log.info("Movie updated successfully: {}", updated.getTitle());

        return modelMapper.map(updated, MovieDto.class);
    }

    @Transactional
    public void deleteMovie(Long id) {
        log.info("Deleting movie ID: {}", id);

        Movie movie = findMovieById(id);

        List<Long> mediaFileIds = movie.getMedia().stream()
                .map(link -> link.getMediaFile().getId())
                .toList();

        movieRepository.delete(movie);

        movieRepository.flush();

        for (Long fileId : mediaFileIds) {
            fileUploadService.deleteFile(fileId);
        }

        log.info("Movie deleted successfully");
    }

    @Transactional(readOnly = true)
    public Page<MovieDto> searchMovies(String title, Integer year, Integer minDuration, Integer maxDuration, int page, int size) {
        log.debug("Searching movies - title: {}, year: {}", title, year);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        QMovie qMovie = QMovie.movie;
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(title)) {
            builder.and(qMovie.title.containsIgnoreCase(title));
        }

        if (year != null) {
            builder.and(qMovie.year.eq(year));
        }

        if (minDuration != null) {
            builder.and(qMovie.duration.goe(minDuration));
        }

        if (maxDuration != null) {
            builder.and(qMovie.duration.loe(maxDuration));
        }

        Page<Movie> movies = movieRepository.findAll(builder, pageable);

        return movies.map(movie -> modelMapper.map(movie, MovieDto.class));
    }

    public long getMoviesCount() {
        return movieRepository.count();
    }

    private Movie findMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + id));
    }
}
