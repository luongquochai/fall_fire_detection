package com.camai.fall_fire_detection.dto;

public class EventResponse {
    private String messageId;
    private String userId;
    private String deviceId;
    private String eventType;
    private String description;
    private String thumbnailUrl;
    private String status;
    private String createdAt;

    public EventResponse() {}

    public EventResponse(String messageId, String userId, String deviceId, 
                        String eventType, String description, String thumbnailUrl, 
                        String status, String createdAt) {
        this.messageId = messageId;
        this.userId = userId;
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
} 