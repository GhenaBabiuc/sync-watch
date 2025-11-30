package com.example.syncservice.service;

import com.example.syncservice.model.Episode;
import com.example.syncservice.model.Movie;
import com.example.syncservice.model.Season;
import com.example.syncservice.model.Series;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StorageService {

    private final RestTemplate restTemplate;
    private final String storageApiUrl;

    public StorageService(RestTemplate restTemplate, @Value("${app.storage.api.url:http://localhost:8081/api}") String storageApiUrl) {
        this.restTemplate = restTemplate;
        this.storageApiUrl = storageApiUrl;
        log.info("StorageService initialized with API URL: {}", storageApiUrl);
    }

    public List<Movie> getAllMovies() {
        try {
            String url = storageApiUrl + "/movies?size=100";
            log.debug("Fetching all movies from: {}", url);

            ResponseEntity<PageResponse<Movie>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<PageResponse<Movie>>() {
                    }
            );

            if (response.getBody() != null && response.getBody().getContent() != null) {
                List<Movie> movies = response.getBody().getContent();
                movies.forEach(this::enrichMovieUrls);
                log.info("Successfully fetched {} movies", movies.size());
                return movies;
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error fetching movies from storage service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<Movie> getMovieById(Long movieId) {
        try {
            String url = storageApiUrl + "/movies/" + movieId;
            log.debug("Fetching movie {} from: {}", movieId, url);

            ResponseEntity<Movie> response = restTemplate.getForEntity(url, Movie.class);

            if (response.getBody() != null) {
                Movie movie = response.getBody();
                enrichMovieUrls(movie);
                return Optional.of(movie);
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Error fetching movie {} from storage service: {}", movieId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Series> getAllSeries() {
        try {
            String url = storageApiUrl + "/series?size=100";
            log.debug("Fetching all series from: {}", url);

            ResponseEntity<PageResponse<Series>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<PageResponse<Series>>() {
                    }
            );

            if (response.getBody() != null && response.getBody().getContent() != null) {
                List<Series> seriesList = response.getBody().getContent();
                seriesList.forEach(this::enrichSeriesUrls);

                seriesList.forEach(s -> {
                    try {
                        List<Season> seasons = getSeasonsBySeries(s.getId());
                        if (!seasons.isEmpty()) {
                            s.setSeasons(seasons);
                        }
                    } catch (Exception e) {
                        log.debug("Could not lazy load seasons for series {}", s.getId());
                    }
                });

                log.info("Successfully fetched {} series", seriesList.size());
                return seriesList;
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error fetching series from storage service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<Series> getSeriesById(Long seriesId) {
        try {
            String url = storageApiUrl + "/series/" + seriesId;
            log.debug("Fetching series {} from: {}", seriesId, url);

            ResponseEntity<Series> response = restTemplate.getForEntity(url, Series.class);

            if (response.getBody() != null) {
                Series series = response.getBody();
                enrichSeriesUrls(series);
                return Optional.of(series);
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Error fetching series {} from storage service: {}", seriesId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Season> getSeasonsBySeries(Long seriesId) {
        try {
            String url = storageApiUrl + "/series/" + seriesId + "/seasons?size=100";
            log.debug("Fetching seasons for series {} from: {}", seriesId, url);

            ResponseEntity<PageResponse<Season>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<PageResponse<Season>>() {
                    }
            );

            if (response.getBody() != null && response.getBody().getContent() != null) {
                List<Season> seasons = response.getBody().getContent();
                return seasons;
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error fetching seasons for series {} from storage service: {}", seriesId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<Season> getSeasonById(Long seasonId) {
        try {
            String url = storageApiUrl + "/series/seasons/" + seasonId;

            ResponseEntity<Season> response = restTemplate.getForEntity(url, Season.class);
            if (response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Error fetching season {}", seasonId, e);
            return Optional.empty();
        }
    }

    public List<Episode> getEpisodesBySeason(Long seasonId) {
        try {
            String url = storageApiUrl + "/series/seasons/" + seasonId + "/episodes?size=100";
            log.debug("Fetching episodes for season {} from: {}", seasonId, url);

            ResponseEntity<PageResponse<Episode>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<PageResponse<Episode>>() {
                    }
            );

            if (response.getBody() != null && response.getBody().getContent() != null) {
                List<Episode> episodes = response.getBody().getContent();

                Optional<Season> seasonOpt = getSeasonById(seasonId);
                seasonOpt.ifPresent(season -> {
                    episodes.forEach(ep -> {
                        ep.setSeasonNumber(season.getSeasonNumber());
                        ep.setSeriesId(season.getSeriesId());
                        enrichEpisodeUrls(ep);
                    });
                });

                return episodes;
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error fetching episodes for season {} from storage service: {}", seasonId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Episode> getAllEpisodesBySeries(Long seriesId) {
        try {
            String url = storageApiUrl + "/series/" + seriesId + "/episodes?size=500";
            log.debug("Fetching all episodes for series {} from: {}", seriesId, url);

            ResponseEntity<PageResponse<Episode>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<PageResponse<Episode>>() {
                    }
            );

            if (response.getBody() != null && response.getBody().getContent() != null) {
                List<Episode> episodes = response.getBody().getContent();
                episodes.forEach(this::enrichEpisodeUrls);

                return episodes;
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error fetching all episodes for series {}: {}", seriesId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<Episode> getEpisodeById(Long episodeId) {
        try {
            String url = storageApiUrl + "/series/episodes/" + episodeId;
            log.debug("Fetching episode {} from: {}", episodeId, url);

            ResponseEntity<Episode> response = restTemplate.getForEntity(url, Episode.class);

            if (response.getBody() != null) {
                Episode episode = response.getBody();
                enrichEpisodeUrls(episode);

                if (episode.getSeasonId() != null) {
                    getSeasonById(episode.getSeasonId()).ifPresent(season -> {
                        episode.setSeasonNumber(season.getSeasonNumber());
                        episode.setSeriesId(season.getSeriesId());
                    });
                }

                return Optional.of(episode);
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Error fetching episode {} from storage service: {}", episodeId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Episode> getFirstEpisode(Long seriesId) {
        List<Season> seasons = getSeasonsBySeries(seriesId);
        if (seasons.isEmpty()) return Optional.empty();

        seasons.sort(Comparator.comparingInt(Season::getSeasonNumber));

        for (Season season : seasons) {
            List<Episode> episodes = getEpisodesBySeason(season.getId());
            if (!episodes.isEmpty()) {
                Episode first = episodes.stream()
                        .min(Comparator.comparingInt(Episode::getEpisodeNumber))
                        .orElse(episodes.get(0));

                first.setSeasonNumber(season.getSeasonNumber());
                first.setSeriesId(seriesId);
                return Optional.of(first);
            }
        }
        return Optional.empty();
    }

    public String getMovieStreamUrl(Long movieId) {
        return storageApiUrl + "/stream/movies/" + movieId;
    }

    public String getEpisodeStreamUrl(Long episodeId) {
        return storageApiUrl + "/stream/episodes/" + episodeId;
    }

    private void enrichMovieUrls(Movie movie) {
        if (movie == null) return;
        movie.setStreamUrl(getMovieStreamUrl(movie.getId()));
        movie.setCoverUrl(storageApiUrl + "/stream/movies/" + movie.getId() + "/cover");
    }

    private void enrichEpisodeUrls(Episode episode) {
        if (episode == null) return;
        episode.setStreamUrl(getEpisodeStreamUrl(episode.getId()));
        episode.setCoverUrl(storageApiUrl + "/stream/episodes/" + episode.getId() + "/cover");
    }

    private void enrichSeriesUrls(Series series) {
        if (series == null) return;
        series.setCoverUrl(storageApiUrl + "/stream/series/" + series.getId() + "/cover");
    }

    public static class PageResponse<T> {
        private List<T> content;
        private int totalElements;
        private int totalPages;
        private int size;
        private int number;

        public List<T> getContent() {
            return content;
        }

        public void setContent(List<T> content) {
            this.content = content;
        }

        public int getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(int totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }
}
