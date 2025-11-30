package com.example.storageservice.controller;

import com.example.storageservice.model.dto.CreateEpisodeRequest;
import com.example.storageservice.model.dto.CreateSeasonRequest;
import com.example.storageservice.model.dto.CreateSeriesRequest;
import com.example.storageservice.model.dto.EpisodeDto;
import com.example.storageservice.model.dto.SeasonDto;
import com.example.storageservice.model.dto.SeriesDto;
import com.example.storageservice.model.dto.UpdateEpisodeRequest;
import com.example.storageservice.model.dto.UpdateSeasonRequest;
import com.example.storageservice.model.dto.UpdateSeriesRequest;
import com.example.storageservice.service.SeriesService;
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
@RequestMapping("/api/series")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class SeriesController {

    private final SeriesService seriesService;

    @GetMapping
    public ResponseEntity<Page<SeriesDto>> getAllSeries(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year) {
        log.info("Getting all series - page: {}, size: {}, title: {}, year: {}", page, size, title, year);
        Page<SeriesDto> series = seriesService.getAllSeries(page, size, title, year);

        return ResponseEntity.ok(series);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeriesDto> getSeriesById(@PathVariable Long id) {
        log.info("Getting series by ID: {}", id);
        SeriesDto series = seriesService.getSeriesById(id);

        return ResponseEntity.ok(series);
    }

    @PostMapping
    public ResponseEntity<SeriesDto> createSeries(@Valid @RequestBody CreateSeriesRequest request) {
        log.info("Creating series: {}", request.getTitle());
        SeriesDto series = seriesService.createSeries(request);

        return ResponseEntity.ok(series);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeriesDto> updateSeries(@PathVariable Long id, @Valid @RequestBody UpdateSeriesRequest request) {
        log.info("Updating series ID: {}", id);
        SeriesDto updated = seriesService.updateSeries(id, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeries(@PathVariable Long id) {
        log.info("Deleting series ID: {}", id);
        seriesService.deleteSeries(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{seriesId}/seasons")
    public ResponseEntity<Page<SeasonDto>> getSeasonsBySeries(
            @PathVariable Long seriesId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Getting seasons for series ID: {}", seriesId);
        Page<SeasonDto> seasons = seriesService.getSeasonsBySeries(seriesId, page, size);

        return ResponseEntity.ok(seasons);
    }

    @GetMapping("/seasons/{seasonId}")
    public ResponseEntity<SeasonDto> getSeasonById(@PathVariable Long seasonId) {
        log.info("Getting season by ID: {}", seasonId);
        SeasonDto season = seriesService.getSeasonById(seasonId);

        return ResponseEntity.ok(season);
    }

    @PostMapping("/{seriesId}/seasons")
    public ResponseEntity<SeasonDto> createSeason(@PathVariable Long seriesId, @Valid @RequestBody CreateSeasonRequest request) {
        log.info("Creating season {} for series {}", request.getSeasonNumber(), seriesId);
        request.setSeriesId(seriesId);
        SeasonDto season = seriesService.createSeason(request);

        return ResponseEntity.ok(season);
    }

    @PutMapping("/seasons/{seasonId}")
    public ResponseEntity<SeasonDto> updateSeason(@PathVariable Long seasonId, @Valid @RequestBody UpdateSeasonRequest request) {
        log.info("Updating season ID: {}", seasonId);
        SeasonDto updated = seriesService.updateSeason(seasonId, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/seasons/{seasonId}")
    public ResponseEntity<Void> deleteSeason(@PathVariable Long seasonId) {
        log.info("Deleting season ID: {}", seasonId);
        seriesService.deleteSeason(seasonId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/seasons/{seasonId}/episodes")
    public ResponseEntity<Page<EpisodeDto>> getEpisodesBySeason(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Getting episodes for season ID: {}", seasonId);
        Page<EpisodeDto> episodes = seriesService.getEpisodesBySeason(seasonId, page, size);

        return ResponseEntity.ok(episodes);
    }

    @GetMapping("/episodes/{episodeId}")
    public ResponseEntity<EpisodeDto> getEpisodeById(@PathVariable Long episodeId) {
        log.info("Getting episode by ID: {}", episodeId);
        EpisodeDto episode = seriesService.getEpisodeById(episodeId);

        return ResponseEntity.ok(episode);
    }

    @PostMapping("/seasons/{seasonId}/episodes")
    public ResponseEntity<EpisodeDto> createEpisode(@PathVariable Long seasonId, @Valid @RequestBody CreateEpisodeRequest request) {
        log.info("Creating episode {} for season {}", request.getEpisodeNumber(), seasonId);
        request.setSeasonId(seasonId);
        EpisodeDto episode = seriesService.createEpisode(request);

        return ResponseEntity.ok(episode);
    }

    @PutMapping("/episodes/{episodeId}")
    public ResponseEntity<EpisodeDto> updateEpisode(@PathVariable Long episodeId, @Valid @RequestBody UpdateEpisodeRequest request) {
        log.info("Updating episode ID: {}", episodeId);
        EpisodeDto updated = seriesService.updateEpisode(episodeId, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/episodes/{episodeId}")
    public ResponseEntity<Void> deleteEpisode(@PathVariable Long episodeId) {
        log.info("Deleting episode ID: {}", episodeId);
        seriesService.deleteEpisode(episodeId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SeriesDto>> searchSeries(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer minSeasons,
            @RequestParam(required = false) Integer maxSeasons,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Searching series with filters - title: {}, year: {}, minSeasons: {}, maxSeasons: {}",
                title, year, minSeasons, maxSeasons);
        Page<SeriesDto> series = seriesService.searchSeries(title, year, minSeasons, maxSeasons, page, size);

        return ResponseEntity.ok(series);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getSeriesCount() {
        log.info("Getting total series count");
        long count = seriesService.getSeriesCount();

        return ResponseEntity.ok(count);
    }

    @GetMapping("/{seriesId}/episodes")
    public ResponseEntity<Page<EpisodeDto>> getAllEpisodesBySeries(
            @PathVariable Long seriesId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        log.info("Getting all episodes for series ID: {}", seriesId);
        Page<EpisodeDto> episodes = seriesService.getAllEpisodesBySeries(seriesId, page, size);

        return ResponseEntity.ok(episodes);
    }
}
