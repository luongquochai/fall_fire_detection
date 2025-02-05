package com.camai.fall_fire_detection.exception;

public class EventException extends RuntimeException {
    public EventException(String message) {
        super(message);
    }
    
    public EventException(String message, Throwable cause) {
        super(message, cause);
    }
} 