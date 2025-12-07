package com.example.syncservice.listener;

import com.example.syncservice.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String eventSessionId = headerAccessor.getSessionId();

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String roomId = (String) sessionAttributes.get("roomId");
        String userId = (String) sessionAttributes.get("userId");

        if (roomId != null && userId != null) {
            roomService.getRoomById(roomId).ifPresent(room -> {
                room.getUsers().stream()
                        .filter(u -> u.getId().equals(userId))
                        .findFirst()
                        .ifPresent(currentUser -> {
                            if (eventSessionId.equals(currentUser.getSessionId())) {
                                log.info("User {} disconnected (session match)", userId);
                                boolean removed = roomService.leaveRoom(roomId, userId);
                                if (removed) {
                                    messagingTemplate.convertAndSend("/topic/room/" + roomId + "/users", room.getUsers());
                                }
                            } else {
                                log.info("Ignored disconnect for User {} (old session: {}, current: {})",
                                        userId, eventSessionId, currentUser.getSessionId());
                            }
                        });
            });
        }
    }
}
