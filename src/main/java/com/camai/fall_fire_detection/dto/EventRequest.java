package com.camai.fall_fire_detection.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    @NotNull
    private String userId;
    
    @NotNull
    private String deviceId;
    
    @NotNull
    private String messageId;
    
    @NotNull
    private String eventType;
    
    private String description;
    private String thumbnailUrl;
    private Instant timestamp;

    // Add getters manually
    public String getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }
    public String getMessageId() { return messageId; }
    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Instant getTimestamp() { return timestamp; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setDescription(String description) { this.description = description; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
} 