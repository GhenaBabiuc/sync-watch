package com.example.storageservice.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final class Video {
        public static final long MAX_VIDEO_FILE_SIZE = 20L * 1024 * 1024 * 1024; // 20GB
        public static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
                ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm");

        private Video() {
        }
    }

    public static final class Image {
        public static final long MAX_IMAGE_FILE_SIZE = 10L * 1024 * 1024; // 10MB
        public static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
                ".jpg", ".jpeg", ".png", ".webp");

        private Image() {
        }
    }

    public static final class Storage {
        public static final String MOVIES_STORAGE_PATH = "movies/";
        public static final String EPISODES_STORAGE_PATH = "episodes/";
        public static final String THUMBNAILS_STORAGE_PATH = "thumbnails/";
        public static final String POSTERS_STORAGE_PATH = "posters/";

        private Storage() {
        }
    }

    public static final class Movie {
        public static final int MIN_RELEASE_YEAR = 1900;
        public static final int MAX_RELEASE_YEAR = 2030;
        public static final int MAX_TITLE_LENGTH = 255;
        public static final int MAX_DESCRIPTION_LENGTH = 2000;
        public static final int MAX_GENRE_LENGTH = 100;

        private Movie() {
        }
    }

    public static final class Series {
        public static final int MIN_SEASON_NUMBER = 1;
        public static final int MAX_SEASON_NUMBER = 50;
        public static final int MIN_EPISODE_NUMBER = 1;
        public static final int MAX_EPISODE_NUMBER = 500;
        public static final int MAX_SERIES_TITLE_LENGTH = 255;
        public static final int MAX_SEASON_TITLE_LENGTH = 255;
        public static final int MAX_EPISODE_TITLE_LENGTH = 255;

        private Series() {
        }
    }
}
