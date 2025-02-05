package com.camai.fall_fire_detection.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.CassandraType;

import java.time.Instant;

@Table("events_by_user")
public class Event {
    @PrimaryKey
    private EventKey key;
    
    @Column("message_id")
    private String messageId;
    
    @Column("device_id")
    private String deviceId;
    
    @Column("event_type")
    @CassandraType(type = CassandraType.Name.TEXT)
    private EventType eventType;
    
    @Column("description")
    private String description;
    
    @Column("thumbnail_url")
    private String thumbnailUrl;

    @Column("status")
    @CassandraType(type = CassandraType.Name.TEXT)
    private EventStatus status;
    
    @Column("category")
    @CassandraType(type = CassandraType.Name.INT)
    private Integer category;
    
    @Column("created_at")
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Instant createdAt;

    // Getters and Setters
    public EventKey getKey() { return key; }
    public void setKey(EventKey key) { this.key = key; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}