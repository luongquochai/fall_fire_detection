package com.camai.fall_fire_detection.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.*;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.mapping.CassandraType;

import java.time.Instant;

@PrimaryKeyClass
public class EventKey {
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PARTITIONED)
    private String userId;
    
    @PrimaryKeyColumn(name = "bucket", ordinal = 1, type = PARTITIONED)
    private int bucket;
    
    @PrimaryKeyColumn(name = "event_date", ordinal = 2, type = CLUSTERED, ordering = Ordering.DESCENDING)
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Instant eventDate;
    
    @PrimaryKeyColumn(name = "device_id", ordinal = 3, type = CLUSTERED)
    private String deviceId;
    
    @PrimaryKeyColumn(name = "event_type", ordinal = 4, type = CLUSTERED)
    private EventType eventType;
    
    @PrimaryKeyColumn(name = "category", ordinal = 5, type = CLUSTERED)
    private int category;
    
    @PrimaryKeyColumn(name = "message_id", ordinal = 6, type = CLUSTERED)
    private String messageId;

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public int getBucket() { return bucket; }
    public void setBucket(int bucket) { this.bucket = bucket; }
    
    public Instant getEventDate() { return eventDate; }
    public void setEventDate(Instant eventDate) { this.eventDate = eventDate; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public int getCategory() { return category; }
    public void setCategory(int category) { this.category = category; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
} 