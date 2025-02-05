package com.camai.fall_fire_detection.service;

import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.repository.EventRepository;
import com.camai.fall_fire_detection.repository.CustomEventRepository;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {
    
    private final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);
    
    private final EventRepository eventRepository;
    private final CustomEventRepository customEventRepository;
    private final HazelcastInstance hazelcastInstance;
    
    public EventServiceImpl(
            EventRepository eventRepository,
            CustomEventRepository customEventRepository,
            HazelcastInstance hazelcastInstance) {
        this.eventRepository = eventRepository;
        this.customEventRepository = customEventRepository;
        this.hazelcastInstance = hazelcastInstance;
    }
    
    @Override
    public Event saveEvent(Event event) {
        try {
            logger.info("Attempting to save event: {}", event);
            
            // Thêm validation
            validateEvent(event);
            
            Event savedEvent = eventRepository.save(event);
            logger.info("Successfully saved event with key: {}", savedEvent.getKey());
            
            return savedEvent;
        } catch (Exception e) {
            logger.error("Error saving event: ", e);
            throw e;
        }
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
            
        return customEventRepository.searchEvents(
            userId, deviceIds, eventType, startTime, endTime, pageSize, pagingState);
    }
    
    @Override
    public Event getEventByMessageId(String messageId) {
        try {
            logger.info("Attempting to find event by messageId: {}", messageId);
            
            // Thêm kiểm tra messageId
            if (messageId == null || messageId.trim().isEmpty()) {
                logger.warn("MessageId is null or empty");
                return null;
            }
            
            Event event = eventRepository.findByMessageId(messageId);
            
            // Kiểm tra và xử lý các trường null
            if (event != null && event.getKey() != null) {
                // Đảm bảo các giá trị số không null
                if (event.getCategory() == null) {
                    event.getKey().setCategory(0); // hoặc giá trị mặc định khác
                }
            }
            
            logger.info("Found event: {}", event);
            return event;
            
        } catch (Exception e) {
            logger.error("Error finding event by messageId: {}", messageId, e);
            throw new RuntimeException("Error retrieving event", e);
        }
    }
    
    private void validateEvent(Event event) {
        if (event.getDeviceId() == null) {
            throw new IllegalArgumentException("Device ID cannot be null");
        }
        // Thêm các validation khác nếu cần
    }
} 