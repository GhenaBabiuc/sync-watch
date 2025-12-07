package com.example.syncservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
public class Room {
    private String id;
    private String name;
    private RoomType roomType;
    private Long contentId;
    private String contentTitle;
    private Long currentSeasonId;
    private Long currentEpisodeId;
    private String customUrl;
    private double currentTime;
    private boolean isPlaying;
    private String hostId;
    private LocalDateTime createdAt;
    private Set<User> users;
    private String lastActionUserId;

    public Room(String id, String name, Long movieId, String movieTitle, String hostId) {
        this.id = id;
        this.name = name;
        this.contentId = movieId;
        this.contentTitle = movieTitle;
        this.roomType = RoomType.MOVIE;
        this.hostId = hostId;
        this.users = ConcurrentHashMap.newKeySet();
        this.createdAt = LocalDateTime.now();
        this.currentTime = 0.0;
        this.isPlaying = false;
    }

    public Room(String id, String name, Long seriesId, String seriesTitle, Long seasonId, Long episodeId, String hostId) {
        this.id = id;
        this.name = name;
        this.contentId = seriesId;
        this.contentTitle = seriesTitle;
        this.currentSeasonId = seasonId;
        this.currentEpisodeId = episodeId;
        this.roomType = RoomType.SERIES;
        this.hostId = hostId;
        this.users = ConcurrentHashMap.newKeySet();
        this.createdAt = LocalDateTime.now();
        this.currentTime = 0.0;
        this.isPlaying = false;
    }

    public Room(String id, String name, String customUrl, String hostId) {
        this.id = id;
        this.name = name;
        this.customUrl = customUrl;
        this.roomType = RoomType.CUSTOM;
        this.contentTitle = "Web Content";
        this.hostId = hostId;
        this.users = ConcurrentHashMap.newKeySet();
        this.createdAt = LocalDateTime.now();
        this.currentTime = 0.0;
        this.isPlaying = false;
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    public void removeUser(User user) {
        this.users.remove(user);
    }

    public int getUserCount() {
        return users.size();
    }

    public enum RoomType {
        MOVIE, SERIES, CUSTOM
    }
}
