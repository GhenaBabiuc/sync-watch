package com.example.syncservice.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import com.example.syncservice.model.Episode;
import com.example.syncservice.model.Movie;
import com.example.syncservice.model.Room;
import com.example.syncservice.model.Season;
import com.example.syncservice.model.Series;
import com.example.syncservice.model.User;
import com.example.syncservice.service.RoomService;
import com.example.syncservice.service.StorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StorageService storageService;
    private final RoomService roomService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        List<Movie> movies = storageService.getAllMovies();
        List<Series> series = storageService.getAllSeries();
        List<Room> rooms = roomService.getAllRooms();

        Map<String, Long> movieRoomCounts = new HashMap<>();
        for (Movie movie : movies) {
            long count = roomService.getRoomsByMovieId(movie.getId()).size();
            movieRoomCounts.put("movie_" + movie.getId(), count);
        }

        Map<String, Long> seriesRoomCounts = new HashMap<>();
        for (Series s : series) {
            long count = roomService.getRoomsBySeriesId(s.getId()).size();
            seriesRoomCounts.put("series_" + s.getId(), count);
        }

        model.addAttribute("movies", movies);
        model.addAttribute("series", series);
        model.addAttribute("rooms", rooms);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("movieRoomCounts", movieRoomCounts);
        model.addAttribute("seriesRoomCounts", seriesRoomCounts);

        return "index";
    }

    @PostMapping("/create-movie-room")
    public String createMovieRoom(@RequestParam Long movieId,
                                  @RequestParam String roomName,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(session);
        try {
            Room room = roomService.createMovieRoom(roomName, movieId, currentUser.getId());
            roomService.joinRoom(room.getId(), currentUser);
            return "redirect:/room/" + room.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/create-series-room")
    public String createSeriesRoom(@RequestParam Long seriesId,
                                   @RequestParam String roomName,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(session);
        try {
            Room room = roomService.createSeriesRoom(roomName, seriesId, currentUser.getId());
            roomService.joinRoom(room.getId(), currentUser);
            return "redirect:/room/" + room.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/create-custom-room")
    public String createCustomRoom(@RequestParam String roomName,
                                   @RequestParam String videoUrl,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(session);
        try {
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("URL cannot be empty");
            }

            Room room = roomService.createCustomRoom(roomName, videoUrl, currentUser.getId());
            roomService.joinRoom(room.getId(), currentUser);
            return "redirect:/room/" + room.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/room/{roomId}")
    public String room(@PathVariable String roomId, Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        Room room = roomService.getRoomById(roomId).orElse(null);
        if (room == null) {
            model.addAttribute("error", "Room not found");
            return "error";
        }

        if (!room.getUsers().contains(currentUser)) {
            roomService.joinRoom(roomId, currentUser);
        }

        model.addAttribute("room", room);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isHost", roomService.isHost(roomId, currentUser.getId()));
        model.addAttribute("streamUrl", room.getStreamUrl());

        if (room.getRoomType() == Room.RoomType.SERIES) {
            List<Season> availableSeasons = roomService.getAvailableSeasons(roomId);
            Map<Long, List<Episode>> episodesBySeason = roomService.getEpisodesBySeason(roomId);

            model.addAttribute("availableSeasons", availableSeasons);
            model.addAttribute("episodesBySeason", episodesBySeason);
            model.addAttribute("currentEpisode", room.getCurrentEpisode());
            if (room.getCurrentEpisode() != null) {
                model.addAttribute("currentSeasonId", room.getCurrentEpisode().getSeasonId());
            }
        }

        return "room";
    }

    @PostMapping("/join-room/{roomId}")
    public String joinRoom(@PathVariable String roomId, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (!roomService.joinRoom(roomId, currentUser)) {
            return "redirect:/?error=room-not-found";
        }

        return "redirect:/room/" + roomId;
    }

    @PostMapping("/leave-room/{roomId}")
    public String leaveRoom(@PathVariable String roomId, HttpSession session) {
        User currentUser = getCurrentUser(session);
        roomService.leaveRoom(roomId, currentUser.getId());
        return "redirect:/";
    }

    @PostMapping("/room/{roomId}/switch-episode")
    @ResponseBody
    public Map<String, Object> switchEpisode(@PathVariable String roomId,
                                             @RequestParam Long episodeId,
                                             HttpSession session) {
        User currentUser = getCurrentUser(session);
        boolean success = roomService.switchEpisode(roomId, episodeId, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            Room room = roomService.getRoomById(roomId).orElse(null);
            if (room != null && room.getCurrentEpisode() != null) {
                response.put("episode", room.getCurrentEpisode());
                response.put("streamUrl", room.getStreamUrl());
            }
        }

        return response;
    }

    @PostMapping("/room/{roomId}/switch-season")
    @ResponseBody
    public Map<String, Object> switchSeason(@PathVariable String roomId,
                                            @RequestParam Long seasonId,
                                            HttpSession session) {
        User currentUser = getCurrentUser(session);
        boolean success = roomService.switchToSeason(roomId, seasonId, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            Room room = roomService.getRoomById(roomId).orElse(null);
            if (room != null && room.getCurrentEpisode() != null) {
                response.put("episode", room.getCurrentEpisode());
                response.put("streamUrl", room.getStreamUrl());
                response.put("seasonId", seasonId);
            }
        }

        return response;
    }

    @PostMapping("/room/{roomId}/next-episode")
    @ResponseBody
    public Map<String, Object> nextEpisode(@PathVariable String roomId, HttpSession session) {
        User currentUser = getCurrentUser(session);
        boolean success = roomService.switchToNextEpisode(roomId, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            Room room = roomService.getRoomById(roomId).orElse(null);
            if (room != null && room.getCurrentEpisode() != null) {
                response.put("episode", room.getCurrentEpisode());
                response.put("streamUrl", room.getStreamUrl());
            }
        }

        return response;
    }

    @PostMapping("/room/{roomId}/previous-episode")
    @ResponseBody
    public Map<String, Object> previousEpisode(@PathVariable String roomId, HttpSession session) {
        User currentUser = getCurrentUser(session);
        boolean success = roomService.switchToPreviousEpisode(roomId, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            Room room = roomService.getRoomById(roomId).orElse(null);
            if (room != null && room.getCurrentEpisode() != null) {
                response.put("episode", room.getCurrentEpisode());
                response.put("streamUrl", room.getStreamUrl());
            }
        }

        return response;
    }

    @GetMapping("/room/{roomId}/episodes-by-season/{seasonId}")
    @ResponseBody
    public Map<String, Object> getEpisodesBySeason(@PathVariable String roomId,
                                                   @PathVariable Long seasonId,
                                                   HttpSession session) {
        User currentUser = getCurrentUser(session);
        List<Episode> episodes = storageService.getEpisodesBySeason(seasonId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("episodes", episodes);
        response.put("seasonId", seasonId);

        return response;
    }

    @GetMapping("/set-username")
    public String setUsernameForm(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);
        return "set-username";
    }

    @PostMapping("/set-username")
    public String setUsername(@RequestParam String username, HttpSession session) {
        User currentUser = getCurrentUser(session);
        currentUser.setUsername(username);
        session.setAttribute("user", currentUser);
        return "redirect:/";
    }

    private User getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            user = new User(UUID.randomUUID().toString(), "Guest" + System.currentTimeMillis() % 1000);
            session.setAttribute("user", user);
        }
        return user;
    }
}
