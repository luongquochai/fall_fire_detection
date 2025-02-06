package com.camai.fall_fire_detection.repository;

import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends CassandraRepository<Event, EventKey> {
    
    @Query("SELECT * FROM events_by_user WHERE user_id = ?0 AND bucket = ?1 AND event_date >= ?2 AND event_date <= ?3")
    List<Event> findByUserIdAndTimeRange(String userId, int bucket, Instant startTime, Instant endTime);
    
    @Query("SELECT * FROM events_by_user WHERE user_id = ?0 AND bucket = ?1 AND device_id IN ?2 AND event_type = ?3")
    List<Event> findByUserIdAndDevicesAndType(String userId, int bucket, List<String> deviceIds, EventType eventType);
    
    @Query("SELECT * FROM events_by_message WHERE message_id = ?0 LIMIT 1")
    Event findByMessageId(String messageId);
} 