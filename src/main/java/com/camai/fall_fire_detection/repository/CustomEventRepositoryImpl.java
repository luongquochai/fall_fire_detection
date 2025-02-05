package com.camai.fall_fire_detection.repository;

import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventKey;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventStatus;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Base64;

@Repository
public class CustomEventRepositoryImpl implements CustomEventRepository {
    
    private final CassandraTemplate cassandraTemplate;
    private final CqlSession session;
    
    public CustomEventRepositoryImpl(CassandraTemplate cassandraTemplate, CqlSession session) {
        this.cassandraTemplate = cassandraTemplate;
        this.session = session;
    }
    
    @Override
    public Slice<Event> searchEvents(String userId, 
                                   List<String> deviceIds, 
                                   EventType eventType,
                                   Instant startTime, 
                                   Instant endTime, 
                                   int pageSize, 
                                   String pagingState) {
        
        String cql = "SELECT * FROM events_by_user WHERE user_id = ? AND bucket = ? " +
                    "AND device_id IN ? AND event_type = ? " +
                    "AND event_date >= ? AND event_date <= ?";
                    
        int bucket = Math.abs(userId.hashCode() % 10);
        
        SimpleStatement statement = SimpleStatement.builder(cql)
            .addPositionalValues(userId, bucket, deviceIds, eventType, startTime, endTime)
            .setPageSize(pageSize)
            .build();
            
        if (pagingState != null) {
            statement = statement.setPagingState(
                com.datastax.oss.protocol.internal.util.Bytes.fromHexString(pagingState)
            );
        }
            
        ResultSet rs = session.execute(statement);
        List<Event> events = new ArrayList<>();
        
        rs.forEach(row -> events.add(mapRowToEvent(row)));
        
        String nextPageState = null;
        if (rs.getExecutionInfo().getPagingState() != null) {
            nextPageState = rs.getExecutionInfo().getPagingState().toString();
        }
        
        return new SliceImpl<>(events, null, nextPageState != null);
    }
    
    @Override
    public void saveEventAsync(Event event) {
        CompletableFuture.runAsync(() -> {
            // Save to events_by_user
            cassandraTemplate.insert(event);
            
            // Save to events_by_device_event
            String insertDeviceEvent = "INSERT INTO events_by_device_event (user_id, device_id, event_type, message_id, description, thumbnail_url, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            session.execute(insertDeviceEvent, 
                event.getKey().getUserId(),
                event.getDeviceId(),
                event.getEventType(),
                event.getMessageId(),
                event.getDescription(),
                event.getThumbnailUrl(),
                event.getStatus(),
                event.getCreatedAt()
            );
            
            // Save to events_by_message
            String insertMessage = "INSERT INTO events_by_message (message_id, user_id, device_id, event_type, description, thumbnail_url, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            session.execute(insertMessage,
                event.getMessageId(),
                event.getKey().getUserId(),
                event.getDeviceId(),
                event.getEventType(),
                event.getDescription(),
                event.getThumbnailUrl(),
                event.getStatus(),
                event.getCreatedAt()
            );
        });
    }
    
    private Event mapRowToEvent(Row row) {
        Event event = new Event();
        EventKey key = new EventKey();
        
        key.setUserId(row.getString("user_id"));
        key.setBucket(row.getInt("bucket"));
        key.setEventDate(row.getInstant("event_date"));
        key.setDeviceId(row.getString("device_id"));
        key.setEventType(EventType.valueOf(row.getString("event_type")));
        key.setCategory(row.getInt("category"));
        key.setMessageId(row.getString("message_id"));
        
        event.setKey(key);
        event.setMessageId(row.getString("message_id"));
        event.setDeviceId(row.getString("device_id"));
        event.setEventType(EventType.valueOf(row.getString("event_type")));
        event.setDescription(row.getString("description"));
        event.setThumbnailUrl(row.getString("thumbnail_url"));
        event.setStatus(EventStatus.valueOf(row.getString("status")));
        event.setCreatedAt(row.getInstant("created_at"));
        
        return event;
    }
} 