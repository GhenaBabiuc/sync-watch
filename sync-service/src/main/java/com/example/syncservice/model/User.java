package com.example.syncservice.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @EqualsAndHashCode.Include
    private String id;
    private String username;
    private String sessionId;
    private double currentTime;
    private LocalDateTime joinedAt;
    private LocalDateTime lastSeen;
    private boolean isConnected;

    public User(String id, String username) {
        this.id = id;
        this.username = username;
        this.joinedAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.isConnected = true;
        this.currentTime = 0.0;
    }
}
