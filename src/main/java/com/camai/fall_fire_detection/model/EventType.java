package com.camai.fall_fire_detection.model;

public enum EventType {
    FALL("FALL"),
    FIRE("FIRE"),
    SMOKE("SMOKE"),
    MOTION_DETECTED("MOTION_DETECTED"),
    FACE_DETECTED("FACE_DETECTED"),
    UNKNOWN("UNKNOWN");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EventType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        for (EventType type : EventType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
} 