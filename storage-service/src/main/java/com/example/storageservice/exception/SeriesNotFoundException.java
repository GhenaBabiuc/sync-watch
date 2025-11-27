package com.example.storageservice.exception;

public class SeriesNotFoundException extends RuntimeException {
    public SeriesNotFoundException(String message) {
        super(message);
    }

    public SeriesNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
