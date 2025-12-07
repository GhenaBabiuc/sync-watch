package com.example.syncservice.controller;

import com.example.syncservice.model.User;
import com.example.syncservice.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

    @MessageMapping("/room/{roomId}/play")
    public void handlePlay(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Double currentTime = ((Number) payload.get("currentTime")).doubleValue();

        roomService.updateRoomState(roomId, currentTime, true, userId);
        roomService.updateAllUsersTime(roomId, currentTime);

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/sync", (Object) Map.of(
                "action", "play",
                "currentTime", currentTime,
                "userId", userId
        ));
    }

    @MessageMapping("/room/{roomId}/pause")
    public void handlePause(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Double currentTime = ((Number) payload.get("currentTime")).doubleValue();
        roomService.updateRoomState(roomId, currentTime, false, userId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/sync", (Object) Map.of(
                "action", "pause",
                "currentTime", currentTime,
                "userId", userId
        ));
    }

    @MessageMapping("/room/{roomId}/seek")
    public void handleSeek(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Double currentTime = ((Number) payload.get("currentTime")).doubleValue();
        roomService.updateRoomState(roomId, currentTime, false, userId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/sync", (Object) Map.of(
                "action", "seek",
                "currentTime", currentTime,
                "userId", userId
        ));
    }

    @MessageMapping("/room/{roomId}/timeUpdate")
    public void handleTimeUpdate(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Double currentTime = ((Number) payload.get("currentTime")).doubleValue();
        roomService.updateUserTime(roomId, userId, currentTime);
        updateRoomUsers(roomId);
    }

    @MessageMapping("/room/{roomId}/join")
    public void handleJoin(@DestinationVariable String roomId,
                           @Payload Map<String, Object> payload,
                           org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) payload.get("userId");
        String username = (String) payload.get("username");

        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("roomId", roomId);
            headerAccessor.getSessionAttributes().put("userId", userId);
        }

        User user = new User(userId, username);
        user.setSessionId(headerAccessor.getSessionId());

        roomService.joinRoom(roomId, user);
        updateRoomUsers(roomId);

        roomService.getRoomById(roomId).ifPresent(room -> {
            messagingTemplate.convertAndSendToUser(userId, "/queue/room/" + roomId + "/state", Map.of(
                    "currentTime", room.getCurrentTime(),
                    "isPlaying", room.isPlaying(),
                    "currentEpisodeId", room.getCurrentEpisodeId() != null ? room.getCurrentEpisodeId() : 0,
                    "customUrl", room.getCustomUrl() != null ? room.getCustomUrl() : ""
            ));
        });
    }

    @MessageMapping("/room/{roomId}/leave")
    public void handleLeave(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        roomService.leaveRoom(roomId, userId);
        updateRoomUsers(roomId);
    }

    @MessageMapping("/room/{roomId}/switchEpisode")
    public void handleSwitchEpisode(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");

        Long episodeId = ((Number) payload.get("episodeId")).longValue();
        Long seasonId = ((Number) payload.get("seasonId")).longValue();

        if (!roomService.isHost(roomId, userId)) {
            return;
        }

        boolean success = roomService.setRoomEpisode(roomId, seasonId, episodeId, userId);

        if (success) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/episodeChanged", (Object) Map.of(
                    "action", "episodeChanged",
                    "episodeId", episodeId,
                    "seasonId", seasonId,
                    "userId", userId
            ));
        }
    }

    private void updateRoomUsers(String roomId) {
        roomService.getRoomById(roomId).ifPresent(room -> {
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/users", room.getUsers());
        });
    }
}
