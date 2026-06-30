package com.example.geocoder.infrastructure.exception;

/**
 * Исключение для случая, когда сущность не найдена (404)
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}