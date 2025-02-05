package com.camai.fall_fire_detection.repository;

import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventType;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.List;

public interface CustomEventRepository {
    Slice<Event> searchEvents(String userId, 
                            List<String> deviceIds, 
                            EventType eventType,
                            Instant startTime, 
                            Instant endTime, 
                            int pageSize, 
                            String pagingState);
                            
    void saveEventAsync(Event event);
} 