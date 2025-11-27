package com.example.storageservice.service;

import com.example.storageservice.exception.SeriesNotFoundException;
import com.example.storageservice.model.Episode;
import com.example.storageservice.model.Season;
import com.example.storageservice.model.Series;
import com.example.storageservice.model.dto.CreateEpisodeRequest;
import com.example.storageservice.model.dto.CreateSeasonRequest;
import com.example.storageservice.model.dto.CreateSeriesRequest;
import com.example.storageservice.model.dto.EpisodeDto;
import com.example.storageservice.model.dto.FileInfoDto;
import com.example.storageservice.model.dto.FileUploadRequest;
import com.example.storageservice.model.dto.SeasonDto;
import com.example.storageservice.model.dto.SeriesDto;
import com.example.storageservice.model.dto.UpdateEpisodeRequest;
import com.example.storageservice.model.dto.UpdateSeasonRequest;
import com.example.storageservice.model.dto.UpdateSeriesRequest;
import com.example.storageservice.repository.EpisodeRepository;
import com.example.storageservice.repository.SeasonRepository;
import com.example.storageservice.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final FileUploadService fileUploadService;

    public Page<SeriesDto> getAllSeries(int page, int size, String title, Integer year) {
        log.debug("Getting all series - page: {}, size: {}, title: {}, year: {}", page, size, title, year);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Series> series = seriesRepository.findSeriesWithFilters(title, year, pageable);

        return series.map(this::mapSeriesToDto);
    }

    public SeriesDto getSeriesById(Long id) {
        log.debug("Getting series by ID: {}", id);
        Series series = findSeriesById(id);
        return mapSeriesToDto(series);
    }

    @Transactional
    public SeriesDto createSeries(CreateSeriesRequest request) {
        log.info("Creating new series: {}", request.getTitle());

        Series series = Series.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .year(request.getYear())
                .build();

        Series saved = seriesRepository.save(series);
        log.info("Series created successfully with ID: {}", saved.getId());

        return mapSeriesToDto(saved);
    }

    @Transactional
    public SeriesDto updateSeries(Long id, UpdateSeriesRequest request) {
        log.info("Updating series ID: {}", id);

        Series series = findSeriesById(id);

        if (request.getTitle() != null) {
            series.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            series.setDescription(request.getDescription());
        }
        if (request.getYear() != null) {
            series.setYear(request.getYear());
        }

        Series updated = seriesRepository.save(series);
        log.info("Series updated successfully: {}", updated.getTitle());

        return mapSeriesToDto(updated);
    }

    @Transactional
    public void deleteSeries(Long id) {
        log.info("Deleting series ID: {}", id);

        Series series = findSeriesById(id);

        List<Episode> episodes = episodeRepository.findBySeriesId(id);
        for (Episode episode : episodes) {
            List<FileInfoDto> files = fileUploadService.getFilesByEpisode(episode.getId());
            for (FileInfoDto file : files) {
                try {
                    fileUploadService.deleteFile(file.getId(), FileUploadRequest.EntityType.EPISODE);
                    log.debug("Deleted episode file: {}", file.getOriginalFilename());
                } catch (Exception e) {
                    log.error("Error deleting episode file {}: {}", file.getOriginalFilename(), e.getMessage());
                }
            }
        }

        seriesRepository.delete(series);
        log.info("Series deleted successfully: {}", series.getTitle());
    }

    public Page<SeasonDto> getSeasonsBySeries(Long seriesId, int page, int size) {
        log.debug("Getting seasons for series ID: {}", seriesId);

        Series series = findSeriesById(seriesId);
        List<Season> seasons = seasonRepository.findBySeriesIdOrderBySeasonNumber(seriesId);

        int start = page * size;
        int end = Math.min((start + size), seasons.size());
        List<Season> seasonPage = seasons.subList(start, end);

        List<SeasonDto> seasonDtos = seasonPage.stream()
                .map(this::mapSeasonToDto)
                .toList();

        return new PageImpl<>(seasonDtos, PageRequest.of(page, size), seasons.size());
    }

    public SeasonDto getSeasonById(Long seasonId) {
        log.debug("Getting season by ID: {}", seasonId);
        Season season = findSeasonById(seasonId);
        return mapSeasonToDto(season);
    }

    @Transactional
    public SeasonDto createSeason(CreateSeasonRequest request) {
        log.info("Creating season {} for series {}", request.getSeasonNumber(), request.getSeriesId());

        Series series = findSeriesById(request.getSeriesId());

        if (seasonRepository.existsBySeriesIdAndSeasonNumber(request.getSeriesId(), request.getSeasonNumber())) {
            throw new IllegalArgumentException("Season " + request.getSeasonNumber() +
                    " already exists for this series");
        }

        Season season = Season.builder()
                .seriesId(request.getSeriesId())
                .seasonNumber(request.getSeasonNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        Season saved = seasonRepository.save(season);
        log.info("Season created successfully: S{} for series: {}", saved.getSeasonNumber(), series.getTitle());

        return mapSeasonToDto(saved);
    }

    @Transactional
    public SeasonDto updateSeason(Long seasonId, UpdateSeasonRequest request) {
        log.info("Updating season ID: {}", seasonId);

        Season season = findSeasonById(seasonId);

        if (request.getTitle() != null) {
            season.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            season.setDescription(request.getDescription());
        }

        Season updated = seasonRepository.save(season);
        log.info("Season updated successfully: S{}", updated.getSeasonNumber());

        return mapSeasonToDto(updated);
    }

    @Transactional
    public void deleteSeason(Long seasonId) {
        log.info("Deleting season ID: {}", seasonId);

        Season season = findSeasonById(seasonId);

        List<Episode> episodes = episodeRepository.findBySeasonIdOrderByEpisodeNumber(seasonId);
        for (Episode episode : episodes) {
            List<FileInfoDto> files = fileUploadService.getFilesByEpisode(episode.getId());
            for (FileInfoDto file : files) {
                try {
                    fileUploadService.deleteFile(file.getId(), FileUploadRequest.EntityType.EPISODE);
                    log.debug("Deleted episode file: {}", file.getOriginalFilename());
                } catch (Exception e) {
                    log.error("Error deleting episode file {}: {}", file.getOriginalFilename(), e.getMessage());
                }
            }
        }

        seasonRepository.delete(season);
        log.info("Season deleted successfully: S{}", season.getSeasonNumber());
    }

    public Page<EpisodeDto> getEpisodesBySeason(Long seasonId, int page, int size) {
        log.debug("Getting episodes for season ID: {}", seasonId);

        Season season = findSeasonById(seasonId);
        List<Episode> episodes = episodeRepository.findBySeasonIdOrderByEpisodeNumber(seasonId);

        int start = page * size;
        int end = Math.min((start + size), episodes.size());
        List<Episode> episodePage = episodes.subList(start, end);

        List<EpisodeDto> episodeDtos = episodePage.stream()
                .map(this::mapEpisodeToDto)
                .toList();

        return new PageImpl<>(episodeDtos, PageRequest.of(page, size), episodes.size());
    }

    public EpisodeDto getEpisodeById(Long episodeId) {
        log.debug("Getting episode by ID: {}", episodeId);
        Episode episode = findEpisodeById(episodeId);
        return mapEpisodeToDto(episode);
    }

    @Transactional
    public EpisodeDto createEpisode(CreateEpisodeRequest request) {
        log.info("Creating episode {} for season {}", request.getEpisodeNumber(), request.getSeasonId());

        Season season = findSeasonById(request.getSeasonId());

        if (episodeRepository.existsBySeasonIdAndEpisodeNumber(request.getSeasonId(), request.getEpisodeNumber())) {
            throw new IllegalArgumentException("Episode " + request.getEpisodeNumber() +
                    " already exists for this season");
        }

        Episode episode = Episode.builder()
                .seasonId(request.getSeasonId())
                .episodeNumber(request.getEpisodeNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(request.getDuration())
                .build();

        Episode saved = episodeRepository.save(episode);
        log.info("Episode created successfully: S{}E{} - {}",
                season.getSeasonNumber(), saved.getEpisodeNumber(), saved.getTitle());

        return mapEpisodeToDto(saved);
    }

    @Transactional
    public EpisodeDto updateEpisode(Long episodeId, UpdateEpisodeRequest request) {
        log.info("Updating episode ID: {}", episodeId);

        Episode episode = findEpisodeById(episodeId);

        if (request.getTitle() != null) {
            episode.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            episode.setDescription(request.getDescription());
        }
        if (request.getDuration() != null) {
            episode.setDuration(request.getDuration());
        }

        Episode updated = episodeRepository.save(episode);
        log.info("Episode updated successfully: S{}E{} - {}",
                updated.getSeason() != null ? updated.getSeason().getSeasonNumber() : "?",
                updated.getEpisodeNumber(), updated.getTitle());

        return mapEpisodeToDto(updated);
    }

    @Transactional
    public void deleteEpisode(Long episodeId) {
        log.info("Deleting episode ID: {}", episodeId);

        Episode episode = findEpisodeById(episodeId);

        List<FileInfoDto> files = fileUploadService.getFilesByEpisode(episodeId);
        for (FileInfoDto file : files) {
            try {
                fileUploadService.deleteFile(file.getId(), FileUploadRequest.EntityType.EPISODE);
                log.debug("Deleted episode file: {}", file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Error deleting episode file {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }

        episodeRepository.delete(episode);
        log.info("Episode deleted successfully: S{}E{}",
                episode.getSeason() != null ? episode.getSeason().getSeasonNumber() : "?",
                episode.getEpisodeNumber());
    }

    public Page<SeriesDto> searchSeries(String title, Integer year, Integer minSeasons,
                                        Integer maxSeasons, int page, int size) {
        log.debug("Searching series - title: {}, year: {}, minSeasons: {}, maxSeasons: {}",
                title, year, minSeasons, maxSeasons);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Series> series = seriesRepository.findSeriesWithFilters(title, year, pageable);

        return series.map(this::mapSeriesToDto);
    }

    public long getSeriesCount() {
        log.debug("Getting total series count");
        return seriesRepository.count();
    }

    public Page<EpisodeDto> getAllEpisodesBySeries(Long seriesId, int page, int size) {
        log.debug("Getting all episodes for series ID: {}", seriesId);

        Series series = findSeriesById(seriesId);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Episode> episodes = episodeRepository.findBySeriesId(seriesId, pageable);

        return episodes.map(this::mapEpisodeToDto);
    }

    private Series findSeriesById(Long id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new SeriesNotFoundException("Series not found with id: " + id));
    }

    private Season findSeasonById(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new SeriesNotFoundException("Season not found with id: " + id));
    }

    private Episode findEpisodeById(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new SeriesNotFoundException("Episode not found with id: " + id));
    }

    private SeriesDto mapSeriesToDto(Series series) {
        Integer totalSeasons = seasonRepository.findBySeriesIdOrderBySeasonNumber(series.getId()).size();
        Integer totalEpisodes = episodeRepository.countBySeriesId(series.getId());

        List<Season> seasons = seasonRepository.findBySeriesIdOrderBySeasonNumber(series.getId());
        List<SeasonDto> seasonDtos = seasons.stream()
                .map(season -> {
                    List<Episode> episodes = episodeRepository.findBySeasonIdOrderByEpisodeNumber(season.getId());
                    List<EpisodeDto> episodeDtos = episodes.stream()
                            .limit(1)
                            .map(this::mapEpisodeToDto)
                            .toList();

                    return SeasonDto.builder()
                            .id(season.getId())
                            .seriesId(season.getSeriesId())
                            .seasonNumber(season.getSeasonNumber())
                            .title(season.getTitle())
                            .episodes(episodeDtos)
                            .build();
                })
                .toList();

        return SeriesDto.builder()
                .id(series.getId())
                .title(series.getTitle())
                .description(series.getDescription())
                .year(series.getYear())
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .totalSeasons(totalSeasons)
                .totalEpisodes(totalEpisodes)
                .seasons(seasonDtos)
                .build();
    }

    private SeasonDto mapSeasonToDto(Season season) {
        Integer totalEpisodes = episodeRepository.countBySeasonId(season.getId());

        String seriesTitle = null;
        try {
            Series series = findSeriesById(season.getSeriesId());
            seriesTitle = series.getTitle();
        } catch (Exception e) {
            log.warn("Could not load series title for season {}: {}", season.getId(), e.getMessage());
        }

        return SeasonDto.builder()
                .id(season.getId())
                .seriesId(season.getSeriesId())
                .seasonNumber(season.getSeasonNumber())
                .title(season.getTitle())
                .description(season.getDescription())
                .createdAt(season.getCreatedAt())
                .totalEpisodes(totalEpisodes)
                .seriesTitle(seriesTitle)
                .build();
    }

    private EpisodeDto mapEpisodeToDto(Episode episode) {
        List<FileInfoDto> files = fileUploadService.getFilesByEpisode(episode.getId());

        Integer seasonNumber = null;
        Long seriesId = null;
        String seriesTitle = null;

        try {
            Season season = findSeasonById(episode.getSeasonId());
            seasonNumber = season.getSeasonNumber();
            seriesId = season.getSeriesId();

            Series series = findSeriesById(season.getSeriesId());
            seriesTitle = series.getTitle();
        } catch (Exception e) {
            log.warn("Could not load season/series info for episode {}: {}", episode.getId(), e.getMessage());
        }

        return EpisodeDto.builder()
                .id(episode.getId())
                .seasonId(episode.getSeasonId())
                .episodeNumber(episode.getEpisodeNumber())
                .title(episode.getTitle())
                .description(episode.getDescription())
                .duration(episode.getDuration())
                .createdAt(episode.getCreatedAt())
                .updatedAt(episode.getUpdatedAt())
                .files(files)
                .seasonNumber(seasonNumber)
                .seriesId(seriesId)
                .seriesTitle(seriesTitle)
                .build();
    }
}
