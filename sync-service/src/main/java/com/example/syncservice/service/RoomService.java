package com.example.syncservice.service;

import com.example.syncservice.model.Room;
import com.example.syncservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createMovieRoom(String roomName, Long movieId, String title, String hostId) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        Room room = new Room(roomId, roomName, movieId, title, hostId);
        rooms.put(roomId, room);
        log.info("Movie room created: {}", roomId);
        return room;
    }

    public Room createSeriesRoom(String roomName, Long seriesId, String title, Long seasonId, Long episodeId, String hostId) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        Room room = new Room(roomId, roomName, seriesId, title, seasonId, episodeId, hostId);
        rooms.put(roomId, room);
        log.info("Series room created: {}", roomId);
        return room;
    }

    public Room createCustomRoom(String roomName, String url, String hostId) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
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
            user.setCurrentTime(room.getCurrentTime());
            room.getUsers().remove(user);
            room.addUser(user);
            return true;
        }
        return false;
    }

    public boolean leaveRoom(String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            return room.getUsers().removeIf(user -> user.getId().equals(userId));
        }
        return false;
    }

    public void updateRoomState(String roomId, double currentTime, boolean isPlaying, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.setCurrentTime(currentTime);
            room.setPlaying(isPlaying);
            room.setLastActionUserId(userId);
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

            if (userId.equals(room.getHostId())) {
                room.setCurrentTime(currentTime);
                if (!room.isPlaying()) {
                    room.setPlaying(true);
                }
            }
        }
    }

    public void updateAllUsersTime(String roomId, double currentTime) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.getUsers().forEach(user -> user.setCurrentTime(currentTime));
        }
    }

    public boolean setRoomEpisode(String roomId, Long seasonId, Long episodeId, String userId) {
        Room room = rooms.get(roomId);
        if (room == null || !isHost(roomId, userId)) return false;

        room.setCurrentSeasonId(seasonId);
        room.setCurrentEpisodeId(episodeId);
        room.setCurrentTime(0.0);
        room.setPlaying(false);
        room.setLastActionUserId(userId);

        log.info("Room {} switched to episode ID {}", roomId, episodeId);
        return true;
    }

    public boolean isHost(String roomId, String userId) {
        Room room = rooms.get(roomId);
        return room != null && room.getHostId().equals(userId);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupEmptyRooms() {
        rooms.entrySet().removeIf(entry -> {
            Room room = entry.getValue();
            if (room.getUsers().isEmpty()) {
                log.info("Cleaning up abandoned room: {}", room.getId());
                return true;
            }
            return false;
        });
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupInactiveUsers() {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusMinutes(2);
        rooms.forEach((roomId, room) -> {
            boolean changed = room.getUsers().removeIf(user ->
                    user.getLastSeen() != null && user.getLastSeen().isBefore(threshold)
            );
            if (changed) {
                log.info("Cleaned up inactive users in room: {}", roomId);
            }
        });
    }
}
