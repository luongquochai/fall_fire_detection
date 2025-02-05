package com.camai.fall_fire_detection.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.*;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.CassandraType.Name;

import java.time.Instant;
import java.io.Serializable;

@PrimaryKeyClass
public class EventKey implements Serializable {
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PARTITIONED)
    private String userId;
    
    @PrimaryKeyColumn(name = "bucket", ordinal = 1, type = PARTITIONED)
    @CassandraType(type = Name.INT)
    private Integer bucket = 0;
    
    @PrimaryKeyColumn(name = "event_date", ordinal = 2, type = CLUSTERED, ordering = Ordering.DESCENDING)
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Instant eventDate;
    
    @PrimaryKeyColumn(name = "device_id", ordinal = 3, type = CLUSTERED)
    private String deviceId;
    
    @PrimaryKeyColumn(name = "event_type", ordinal = 4, type = CLUSTERED)
    private EventType eventType;
    
    @PrimaryKeyColumn(name = "category", ordinal = 5, type = CLUSTERED)
    @CassandraType(type = Name.INT)
    private Integer category = 0;
    
    @PrimaryKeyColumn(name = "message_id", ordinal = 6, type = CLUSTERED)
    private String messageId;

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Integer getBucket() {
        return bucket != null ? bucket : 0;
    }
    public void setBucket(Integer bucket) {
        this.bucket = bucket != null ? bucket : 0;
    }
    
    public Instant getEventDate() { return eventDate; }
    public void setEventDate(Instant eventDate) { this.eventDate = eventDate; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public Integer getCategory() {
        return category != null ? category : 0;
    }
    public void setCategory(Integer category) {
        this.category = category != null ? category : 0;
    }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
} 