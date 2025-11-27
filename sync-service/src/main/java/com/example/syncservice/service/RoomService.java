package com.example.syncservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.syncservice.model.Episode;
import com.example.syncservice.model.Movie;
import com.example.syncservice.model.Room;
import com.example.syncservice.model.Season;
import com.example.syncservice.model.Series;
import com.example.syncservice.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final StorageService storageService;

    public Room createMovieRoom(String roomName, Long movieId, String hostId) {
        log.info("Creating movie room '{}' for movie ID: {} by host: {}", roomName, movieId, hostId);
        Optional<Movie> movieOpt = storageService.getMovieById(movieId);
        if (movieOpt.isEmpty()) {
            throw new IllegalArgumentException("Movie not found with id: " + movieId);
        }

        String roomId = generateRoomId();
        Room room = new Room(roomId, roomName, movieOpt.get(), hostId);
        rooms.put(roomId, room);

        log.info("Movie room created: {} - '{}' for movie '{}'", roomId, roomName, movieOpt.get().getTitle());
        return room;
    }

    public Room createSeriesRoom(String roomName, Long seriesId, String hostId) {
        log.info("Creating series room '{}' for series ID: {} by host: {}", roomName, seriesId, hostId);
        Optional<Series> seriesOpt = storageService.getSeriesById(seriesId);
        if (seriesOpt.isEmpty()) {
            throw new IllegalArgumentException("Series not found with id: " + seriesId);
        }

        Optional<Episode> firstEpisodeOpt = storageService.getFirstEpisode(seriesId);
        if (firstEpisodeOpt.isEmpty()) {
            throw new IllegalArgumentException("No episodes found for series: " + seriesId);
        }

        String roomId = generateRoomId();
        Room room = new Room(roomId, roomName, seriesOpt.get(), firstEpisodeOpt.get().getId(), hostId);
        room.setCurrentEpisode(firstEpisodeOpt.get());
        rooms.put(roomId, room);

        log.info("Series room created: {} - '{}' for series '{}' starting with S{}E{}",
                roomId, roomName, seriesOpt.get().getTitle(),
                firstEpisodeOpt.get().getSeasonNumber(),
                firstEpisodeOpt.get().getEpisodeNumber());
        return room;
    }

    public Room createCustomRoom(String roomName, String url, String hostId) {
        log.info("Creating custom room '{}' with url: {} by host: {}", roomName, url, hostId);

        String roomId = generateRoomId();
        Room room = new Room(roomId, roomName, url, hostId);
        rooms.put(roomId, room);

        return room;
    }

    public Optional<Room> getRoomById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public boolean joinRoom(String roomId, User user) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.addUser(user);
            log.info("User '{}' joined room '{}'", user.getUsername(), room.getName());
            return true;
        }
        log.warn("Attempted to join non-existent room: {}", roomId);
        return false;
    }

    public boolean leaveRoom(String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            boolean removed = room.getUsers().removeIf(user -> user.getId().equals(userId));
            if (removed) {
                log.info("User {} left room '{}'", userId, room.getName());
            }

            if (room.getUsers().isEmpty()) {
                rooms.remove(roomId);
                log.info("Deleted empty room: {}", roomId);
            }
            return removed;
        }
        return false;
    }

    public void updateRoomState(String roomId, double currentTime, boolean isPlaying, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.setCurrentTime(currentTime);
            room.setPlaying(isPlaying);
            room.setLastActionUserId(userId);
            log.debug("Room '{}' state updated: time={}, playing={}, by user={}",
                    room.getName(), currentTime, isPlaying, userId);
        }
    }

    public void updateUserTime(String roomId, String userId, double currentTime) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.getUsers().stream()
                    .filter(user -> user.getId().equals(userId))
                    .findFirst()
                    .ifPresent(user -> {
                        user.setCurrentTime(currentTime);
                        user.setLastSeen(java.time.LocalDateTime.now());
                    });
        }
    }

    public void updateAllUsersTime(String roomId, double currentTime) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.getUsers().forEach(user -> {
                user.setCurrentTime(currentTime);
                user.setLastSeen(java.time.LocalDateTime.now());
            });
        }
    }

    public boolean switchEpisode(String roomId, Long episodeId, String userId) {
        log.info("Switching episode in room {} to episode {} by user {}", roomId, episodeId, userId);
        Room room = rooms.get(roomId);
        if (room == null) {
            log.warn("Room not found: {}", roomId);
            return false;
        }

        if (room.getRoomType() != Room.RoomType.SERIES) {
            log.warn("Cannot switch episode in movie/custom room: {}", roomId);
            return false;
        }

        if (!isHost(roomId, userId)) {
            log.warn("User {} is not host of room {}, cannot switch episode", userId, roomId);
            return false;
        }

        Optional<Episode> episodeOpt = storageService.getEpisodeById(episodeId);
        if (episodeOpt.isEmpty()) {
            log.warn("Episode not found: {}", episodeId);
            return false;
        }

        Episode episode = episodeOpt.get();
        if (!episode.getSeriesId().equals(room.getSeries().getId())) {
            log.warn("Episode {} does not belong to series {} in room {}",
                    episodeId, room.getSeries().getId(), roomId);
            return false;
        }

        room.setCurrentEpisodeId(episodeId);
        room.setCurrentEpisode(episode);
        room.setCurrentTime(0.0);
        room.setPlaying(false);
        room.setLastActionUserId(userId);
        log.info("Switched to episode S{}E{} in room '{}' by user {}",
                episode.getSeasonNumber(), episode.getEpisodeNumber(), room.getName(), userId);
        return true;
    }

    public boolean switchToNextEpisode(String roomId, String userId) {
        log.info("Switching to next episode in room {} by user {}", roomId, userId);
        Room room = rooms.get(roomId);
        if (room == null || room.getRoomType() != Room.RoomType.SERIES) {
            return false;
        }

        if (!isHost(roomId, userId)) {
            log.warn("User {} is not host of room {}, cannot switch to next episode", userId, roomId);
            return false;
        }

        Optional<Episode> nextEpisodeOpt = findNextEpisode(room);
        if (nextEpisodeOpt.isEmpty()) {
            log.info("No next episode found for room {}", roomId);
            return false;
        }

        return switchEpisode(roomId, nextEpisodeOpt.get().getId(), userId);
    }

    public boolean switchToPreviousEpisode(String roomId, String userId) {
        log.info("Switching to previous episode in room {} by user {}", roomId, userId);
        Room room = rooms.get(roomId);
        if (room == null || room.getRoomType() != Room.RoomType.SERIES) {
            return false;
        }

        if (!isHost(roomId, userId)) {
            log.warn("User {} is not host of room {}, cannot switch to previous episode", userId, roomId);
            return false;
        }

        Optional<Episode> prevEpisodeOpt = findPreviousEpisode(room);
        if (prevEpisodeOpt.isEmpty()) {
            log.info("No previous episode found for room {}", roomId);
            return false;
        }

        return switchEpisode(roomId, prevEpisodeOpt.get().getId(), userId);
    }

    public boolean switchToSeason(String roomId, Long seasonId, String userId) {
        log.info("Switching to first episode of season {} in room {} by user {}", seasonId, roomId, userId);
        Room room = rooms.get(roomId);
        if (room == null || room.getRoomType() != Room.RoomType.SERIES) {
            return false;
        }

        if (!isHost(roomId, userId)) {
            log.warn("User {} is not host of room {}, cannot switch season", userId, roomId);
            return false;
        }

        List<Episode> episodes = storageService.getEpisodesBySeason(seasonId);
        if (episodes.isEmpty()) {
            log.warn("No episodes found for season: {}", seasonId);
            return false;
        }

        Episode firstEpisode = episodes.stream()
                .min(Comparator.comparingInt(Episode::getEpisodeNumber))
                .orElse(episodes.get(0));
        return switchEpisode(roomId, firstEpisode.getId(), userId);
    }

    private Optional<Episode> findNextEpisode(Room room) {
        Episode currentEpisode = room.getCurrentEpisode();
        if (currentEpisode == null) {
            return Optional.empty();
        }

        List<Episode> seasonEpisodes = storageService.getEpisodesBySeason(currentEpisode.getSeasonId());
        Optional<Episode> nextInSeason = seasonEpisodes.stream()
                .filter(ep -> ep.getEpisodeNumber() == currentEpisode.getEpisodeNumber() + 1)
                .findFirst();
        if (nextInSeason.isPresent()) {
            return nextInSeason;
        }

        List<Season> seriesSeasons = storageService.getSeasonsBySeries(currentEpisode.getSeriesId());
        Optional<Season> nextSeason = seriesSeasons.stream()
                .filter(season -> season.getSeasonNumber() == currentEpisode.getSeasonNumber() + 1)
                .findFirst();
        if (nextSeason.isPresent()) {
            List<Episode> nextSeasonEpisodes = storageService.getEpisodesBySeason(nextSeason.get().getId());
            return nextSeasonEpisodes.stream()
                    .min(Comparator.comparingInt(Episode::getEpisodeNumber));
        }

        return Optional.empty();
    }

    private Optional<Episode> findPreviousEpisode(Room room) {
        Episode currentEpisode = room.getCurrentEpisode();
        if (currentEpisode == null) {
            return Optional.empty();
        }

        List<Episode> seasonEpisodes = storageService.getEpisodesBySeason(currentEpisode.getSeasonId());
        Optional<Episode> prevInSeason = seasonEpisodes.stream()
                .filter(ep -> ep.getEpisodeNumber() == currentEpisode.getEpisodeNumber() - 1)
                .findFirst();
        if (prevInSeason.isPresent()) {
            return prevInSeason;
        }

        List<Season> seriesSeasons = storageService.getSeasonsBySeries(currentEpisode.getSeriesId());
        Optional<Season> prevSeason = seriesSeasons.stream()
                .filter(season -> season.getSeasonNumber() == currentEpisode.getSeasonNumber() - 1)
                .findFirst();
        if (prevSeason.isPresent()) {
            List<Episode> prevSeasonEpisodes = storageService.getEpisodesBySeason(prevSeason.get().getId());
            return prevSeasonEpisodes.stream()
                    .max(Comparator.comparingInt(Episode::getEpisodeNumber));
        }

        return Optional.empty();
    }

    public List<Season> getAvailableSeasons(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null || room.getRoomType() != Room.RoomType.SERIES) {
            return Collections.emptyList();
        }

        List<Season> seasons = storageService.getSeasonsBySeries(room.getSeries().getId());
        log.debug("Found {} seasons for room '{}'", seasons.size(), room.getName());
        return seasons;
    }

    public List<Episode> getAvailableEpisodes(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null || room.getRoomType() != Room.RoomType.SERIES) {
            return Collections.emptyList();
        }

        List<Episode> allEpisodes = storageService.getAllEpisodesBySeries(room.getSeries().getId());
        allEpisodes.sort(Comparator.comparingInt(Episode::getSeasonNumber).thenComparingInt(Episode::getEpisodeNumber));
        log.debug("Found {} episodes for room '{}'", allEpisodes.size(), room.getName());
        return allEpisodes;
    }

    public Map<Long, List<Episode>> getEpisodesBySeason(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null || room.getRoomType() != Room.RoomType.SERIES) {
            return Collections.emptyMap();
        }

        List<Season> seasons = getAvailableSeasons(roomId);
        Map<Long, List<Episode>> episodesBySeasonId = new HashMap<>();
        for (Season season : seasons) {
            List<Episode> episodes = storageService.getEpisodesBySeason(season.getId());
            episodesBySeasonId.put(season.getId(), episodes);
        }

        log.debug("Organized {} seasons with episodes for room '{}'", episodesBySeasonId.size(), room.getName());
        return episodesBySeasonId;
    }

    public List<Room> getRoomsByMovieId(Long movieId) {
        return rooms.values().stream()
                .filter(room -> room.getRoomType() == Room.RoomType.MOVIE)
                .filter(room -> room.getMovie() != null && room.getMovie().getId().equals(movieId))
                .collect(Collectors.toList());
    }

    public List<Room> getRoomsBySeriesId(Long seriesId) {
        return rooms.values().stream()
                .filter(room -> room.getRoomType() == Room.RoomType.SERIES)
                .filter(room -> room.getSeries() != null && room.getSeries().getId().equals(seriesId))
                .collect(Collectors.toList());
    }

    public boolean isHost(String roomId, String userId) {
        Room room = rooms.get(roomId);
        return room != null && room.getHostId().equals(userId);
    }

    public void deleteRoom(String roomId) {
        Room removed = rooms.remove(roomId);
        if (removed != null) {
            log.info("Manually deleted room: '{}'", removed.getName());
        }
    }

    public int getTotalRoomsCount() {
        return rooms.size();
    }

    public int getTotalUsersCount() {
        return rooms.values().stream()
                .mapToInt(Room::getUserCount)
                .sum();
    }

    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
