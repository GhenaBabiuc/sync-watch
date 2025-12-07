package com.example.syncservice.controller;

import com.example.syncservice.dto.CreateMovieRoomRequest;
import com.example.syncservice.dto.CreateSeriesRoomRequest;
import com.example.syncservice.model.Room;
import com.example.syncservice.model.User;
import com.example.syncservice.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        List<Room> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(Map.of("rooms", rooms));
    }

    @PostMapping("/rooms/movie")
    public ResponseEntity<Room> createMovieRoom(@Valid @RequestBody CreateMovieRoomRequest request) {
        Room room = roomService.createMovieRoom(
                request.getRoomName(),
                request.getMovieId(),
                request.getTitle(),
                request.getUserId()
        );
        roomService.joinRoom(room.getId(), new User(request.getUserId(), "Host"));
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @PostMapping("/rooms/series")
    public ResponseEntity<Room> createSeriesRoom(@Valid @RequestBody CreateSeriesRoomRequest request) {
        // Передаем только ID серии и эпизода
        Room room = roomService.createSeriesRoom(
                request.getRoomName(),
                request.getSeriesId(),
                request.getTitle(),
                request.getSeasonId(),
                request.getEpisodeId(),
                request.getUserId()
        );
        roomService.joinRoom(room.getId(), new User(request.getUserId(), "Host"));
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @PostMapping("/rooms/custom")
    public ResponseEntity<Room> createCustomRoom(@RequestBody Map<String, Object> payload) {
        String roomName = (String) payload.get("roomName");
        String videoUrl = (String) payload.get("videoUrl");
        String userId = (String) payload.get("userId");

        Room room = roomService.createCustomRoom(roomName, videoUrl, userId);
        roomService.joinRoom(room.getId(), new User(userId, "Host"));
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Room> getRoom(@PathVariable String roomId) {
        return roomService.getRoomById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId, @RequestParam String userId) {
        roomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/isHost/{roomId}")
    public ResponseEntity<Boolean> isHost(@PathVariable String roomId, @PathVariable String userId) {
        return ResponseEntity.ok(roomService.isHost(roomId, userId));
    }
}
