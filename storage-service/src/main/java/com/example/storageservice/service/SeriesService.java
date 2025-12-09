package com.example.storageservice.service;

import com.example.storageservice.exception.SeriesNotFoundException;
import com.example.storageservice.model.Episode;
import com.example.storageservice.model.EpisodeMedia;
import com.example.storageservice.model.QSeries;
import com.example.storageservice.model.Season;
import com.example.storageservice.model.SeasonMedia;
import com.example.storageservice.model.Series;
import com.example.storageservice.model.SeriesMedia;
import com.example.storageservice.model.dto.CreateEpisodeRequest;
import com.example.storageservice.model.dto.CreateSeasonRequest;
import com.example.storageservice.model.dto.CreateSeriesRequest;
import com.example.storageservice.model.dto.EpisodeDto;
import com.example.storageservice.model.dto.SeasonDto;
import com.example.storageservice.model.dto.SeriesDto;
import com.example.storageservice.model.dto.UpdateEpisodeRequest;
import com.example.storageservice.model.dto.UpdateSeasonRequest;
import com.example.storageservice.model.dto.UpdateSeriesRequest;
import com.example.storageservice.repository.EpisodeRepository;
import com.example.storageservice.repository.EpisodesMediaRepository;
import com.example.storageservice.repository.SeasonRepository;
import com.example.storageservice.repository.SeasonsMediaRepository;
import com.example.storageservice.repository.SeriesMediaRepository;
import com.example.storageservice.repository.SeriesRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final SeriesMediaRepository seriesMediaRepository;
    private final SeasonsMediaRepository seasonsMediaRepository;
    private final EpisodesMediaRepository episodesMediaRepository;
    private final FileUploadService fileUploadService;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public Page<SeriesDto> getAllSeries(int page, int size, String title, Integer year) {
        return searchSeries(title, year, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public SeriesDto getSeriesById(Long id) {
        log.debug("Getting series by ID: {}", id);
        Series series = findSeriesById(id);

        return modelMapper.map(series, SeriesDto.class);
    }

    @Transactional
    public SeriesDto createSeries(CreateSeriesRequest request) {
        log.info("Creating new series: {}", request.getTitle());
        Series series = modelMapper.map(request, Series.class);
        Series saved = seriesRepository.save(series);

        return modelMapper.map(saved, SeriesDto.class);
    }

    @Transactional
    public SeriesDto updateSeries(Long id, UpdateSeriesRequest request) {
        log.info("Updating series ID: {}", id);
        Series series = findSeriesById(id);
        modelMapper.map(request, series);
        Series updated = seriesRepository.save(series);

        return modelMapper.map(updated, SeriesDto.class);
    }

    @Transactional
    public void deleteSeries(Long id) {
        log.info("Deleting series ID: {}", id);
        Series series = findSeriesById(id);

        List<SeriesMedia> seriesMedia = seriesMediaRepository.findBySeriesId(id);
        seriesMedia.forEach(m -> fileUploadService.deleteFile(m.getMediaFile().getId()));

        for (Season season : series.getSeasons()) {
            List<SeasonMedia> seasonMedia = seasonsMediaRepository.findBySeasonId(season.getId());
            seasonMedia.forEach(m -> fileUploadService.deleteFile(m.getMediaFile().getId()));

            for (Episode episode : season.getEpisodes()) {
                List<EpisodeMedia> episodeMedia = episodesMediaRepository.findByEpisodeId(episode.getId());
                episodeMedia.forEach(m -> fileUploadService.deleteFile(m.getMediaFile().getId()));
            }
        }

        seriesRepository.delete(series);
    }

    public Page<SeasonDto> getSeasonsBySeries(Long seriesId, int page, int size) {
        log.debug("Getting seasons for series ID: {}", seriesId);
        findSeriesById(seriesId);

        List<Season> seasons = seasonRepository.findBySeriesIdOrderBySeasonNumber(seriesId);

        int start = Math.min(page * size, seasons.size());
        int end = Math.min((page + 1) * size, seasons.size());

        List<SeasonDto> dtos = seasons.subList(start, end).stream()
                .map(s -> modelMapper.map(s, SeasonDto.class))
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(page, size), seasons.size());
    }

    @Transactional(readOnly = true)
    public SeasonDto getSeasonById(Long seasonId) {
        log.debug("Getting season by ID: {}", seasonId);
        Season season = findSeasonById(seasonId);

        return modelMapper.map(season, SeasonDto.class);
    }

    @Transactional
    public SeasonDto createSeason(CreateSeasonRequest request) {
        log.info("Creating season {} for series {}", request.getSeasonNumber(), request.getSeriesId());
        if (seasonRepository.existsBySeriesIdAndSeasonNumber(request.getSeriesId(), request.getSeasonNumber())) {
            throw new IllegalArgumentException("Season already exists");
        }
        Season season = modelMapper.map(request, Season.class);
        Season saved = seasonRepository.save(season);

        return modelMapper.map(saved, SeasonDto.class);
    }

    @Transactional
    public SeasonDto updateSeason(Long seasonId, UpdateSeasonRequest request) {
        log.info("Updating season ID: {}", seasonId);
        Season season = findSeasonById(seasonId);
        modelMapper.map(request, season);
        Season updated = seasonRepository.save(season);

        return modelMapper.map(updated, SeasonDto.class);
    }

    @Transactional
    public void deleteSeason(Long seasonId) {
        log.info("Deleting season ID: {}", seasonId);
        Season season = findSeasonById(seasonId);

        List<SeasonMedia> seasonMedia = seasonsMediaRepository.findBySeasonId(seasonId);
        seasonMedia.forEach(m -> fileUploadService.deleteFile(m.getMediaFile().getId()));

        for (Episode episode : season.getEpisodes()) {
            List<EpisodeMedia> episodeMedia = episodesMediaRepository.findByEpisodeId(episode.getId());
            episodeMedia.forEach(m -> fileUploadService.deleteFile(m.getMediaFile().getId()));
        }

        seasonRepository.delete(season);
    }

    public Page<EpisodeDto> getEpisodesBySeason(Long seasonId, int page, int size) {
        log.debug("Getting episodes for season ID: {}", seasonId);
        findSeasonById(seasonId);
        List<Episode> episodes = episodeRepository.findBySeasonIdOrderByEpisodeNumber(seasonId);

        int start = Math.min(page * size, episodes.size());
        int end = Math.min((page + 1) * size, episodes.size());

        List<EpisodeDto> dtos = episodes.subList(start, end).stream()
                .map(e -> modelMapper.map(e, EpisodeDto.class))
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(page, size), episodes.size());
    }

    @Transactional(readOnly = true)
    public EpisodeDto getEpisodeById(Long episodeId) {
        log.debug("Getting episode by ID: {}", episodeId);

        return modelMapper.map(findEpisodeById(episodeId), EpisodeDto.class);
    }

    @Transactional
    public EpisodeDto createEpisode(CreateEpisodeRequest request) {
        log.info("Creating episode {} for season {}", request.getEpisodeNumber(), request.getSeasonId());
        if (episodeRepository.existsBySeasonIdAndEpisodeNumber(request.getSeasonId(), request.getEpisodeNumber())) {
            throw new IllegalArgumentException("Episode already exists");
        }
        Episode episode = modelMapper.map(request, Episode.class);
        Episode saved = episodeRepository.save(episode);

        return modelMapper.map(saved, EpisodeDto.class);
    }

    @Transactional
    public EpisodeDto updateEpisode(Long episodeId, UpdateEpisodeRequest request) {
        log.info("Updating episode ID: {}", episodeId);
        Episode episode = findEpisodeById(episodeId);
        modelMapper.map(request, episode);
        Episode updated = episodeRepository.save(episode);

        return modelMapper.map(updated, EpisodeDto.class);
    }

    @Transactional
    public void deleteEpisode(Long episodeId) {
        log.info("Deleting episode ID: {}", episodeId);
        Episode episode = findEpisodeById(episodeId);
        List<EpisodeMedia> media = episodesMediaRepository.findByEpisodeId(episodeId);
        media.forEach(m -> fileUploadService.deleteFile(m.getMediaFile().getId()));
        episodeRepository.delete(episode);
    }

    public Page<SeriesDto> searchSeries(String title, Integer year, Integer minSeasons, Integer maxSeasons, int page, int size) {
        log.debug("Searching series - title: {}, year: {}, minSeasons: {}, maxSeasons: {}", title, year, minSeasons, maxSeasons);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        QSeries qSeries = QSeries.series;
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(title)) {
            builder.and(qSeries.title.containsIgnoreCase(title));
        }

        if (year != null) {
            builder.and(qSeries.year.eq(year));
        }

        Page<Series> series = seriesRepository.findAll(builder, pageable);

        return series.map(s -> modelMapper.map(s, SeriesDto.class));
    }

    public long getSeriesCount() {
        return seriesRepository.count();
    }

    public Page<EpisodeDto> getAllEpisodesBySeries(Long seriesId, int page, int size) {
        log.debug("Getting all episodes for series ID: {}", seriesId);

        return episodeRepository.findBySeriesId(seriesId, PageRequest.of(page, size))
                .map(e -> modelMapper.map(e, EpisodeDto.class));
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
}
