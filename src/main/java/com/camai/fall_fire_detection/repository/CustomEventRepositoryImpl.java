package com.camai.fall_fire_detection.repository;

import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventKey;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventStatus;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import com.datastax.oss.driver.api.core.ConsistencyLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Base64;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Repository
public class CustomEventRepositoryImpl implements CustomEventRepository {
    
    private final CassandraTemplate cassandraTemplate;
    private final CqlSession session;
    
    public CustomEventRepositoryImpl(CassandraTemplate cassandraTemplate, CqlSession session) {
        this.cassandraTemplate = cassandraTemplate;
        this.session = session;
    }
    
    @Override
    public Slice<Event> searchEvents(
            String userId,
            List<String> deviceIds,
            EventType eventType,
            Instant startTime,
            Instant endTime,
            int pageSize,
            String pagingState) {

        StringBuilder cql;
        MapBuilder<String, Object> params = new MapBuilder<>();

        // Chọn bảng/view phù hợp dựa trên tham số tìm kiếm
        if (eventType != null && deviceIds != null && deviceIds.size() == 1) {
            // Tìm theo device_id và event_type
            cql = new StringBuilder(
                "SELECT * FROM events_by_device_event WHERE device_id = :deviceId AND event_type = :eventType"
            );
            params.put("deviceId", deviceIds.get(0));
            params.put("eventType", eventType.toString());
            
        } else if (eventType != null) {
            // Tìm theo event_type
            cql = new StringBuilder(
                "SELECT * FROM events_by_event_type WHERE event_type = :eventType"
            );
            params.put("eventType", eventType.toString());
            
        } else {
            // Tìm theo user_id (bảng chính)
            cql = new StringBuilder(
                "SELECT * FROM events_by_user WHERE user_id = :userId AND bucket = :bucket"
            );
            params.put("userId", userId);
            params.put("bucket", 1); // fixed bucket
        }

        // Thêm điều kiện thời gian
        if (startTime != null) {
            cql.append(" AND event_date >= :startTime");
            params.put("startTime", startTime);
        }

        if (endTime != null) {
            cql.append(" AND event_date <= :endTime");
            params.put("endTime", endTime);
        }

        // Thêm điều kiện user_id cho các view nếu cần
        if (!cql.toString().contains("user_id") && userId != null) {
            cql.append(" AND user_id = :userId");
            params.put("userId", userId);
        }

        // Add ALLOW FILTERING if needed
        if (deviceIds != null && deviceIds.size() > 1) {
            cql.append(" AND device_id IN :deviceIds");
            params.put("deviceIds", deviceIds);
            cql.append(" ALLOW FILTERING");
        }

        SimpleStatement statement = SimpleStatement.builder(cql.toString())
            .setPageSize(pageSize)
            .build()
            .setConsistencyLevel(ConsistencyLevel.ONE);

        statement = statement.setNamedValues(params.build());

        if (pagingState != null && !pagingState.isEmpty()) {
            try {
                statement = statement.setPagingState(
                    ByteBuffer.wrap(Base64.getDecoder().decode(pagingState))
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid paging state", e);
            }
        }

        ResultSet rs = session.execute(statement);
        List<Event> events = new ArrayList<>();
        rs.forEach(row -> events.add(mapRowToEvent(row)));

        String nextPageState = null;
        ByteBuffer pagingStateBuffer = rs.getExecutionInfo().getPagingState();
        if (pagingStateBuffer != null) {
            nextPageState = Base64.getEncoder().encodeToString(pagingStateBuffer.array());
        }

        Pageable pageable = PageRequest.of(0, pageSize);
        return new SliceImpl<>(events, pageable, nextPageState != null);
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

    // Helper class for building parameter maps
    private static class MapBuilder<K, V> {
        private final Map<K, V> map = new HashMap<>();
        
        public MapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }
        
        public Map<K, V> build() {
            return map;
        }
    }
} 