package com.camai.fall_fire_detection.controller;

import com.camai.fall_fire_detection.dto.EventRequest;
import com.camai.fall_fire_detection.dto.EventResponse;
import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventKey;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventStatus;
import com.camai.fall_fire_detection.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final Logger logger = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventService eventService;
    
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        try {
            Event event = new Event();
            EventKey key = new EventKey();
            
            // Log request data
            logger.info("Request data: {}", request);
            
            // Đảm bảo timestamp không null
            Instant eventDate = request.getTimestamp() != null ? 
                request.getTimestamp() : Instant.now();
            
            // Set EventKey fields
            key.setUserId(request.getUserId());
            key.setMessageId(request.getMessageId());
            key.setDeviceId(request.getDeviceId());
            key.setEventType(EventType.valueOf(request.getEventType()));
            key.setEventDate(eventDate);  // Sử dụng eventDate đã kiểm tra null
            key.setBucket(1);
            key.setCategory(1);
            
            logger.info("EventKey created: {}", key);
            
            event.setKey(key);
            event.setDeviceId(request.getDeviceId());
            event.setEventType(EventType.valueOf(request.getEventType()));
            event.setDescription(request.getDescription());
            event.setThumbnailUrl(request.getThumbnailUrl());
            event.setCreatedAt(eventDate);  // Sử dụng cùng eventDate
            event.setStatus(EventStatus.PENDING);
            event.setMessageId(request.getMessageId());
            
            logger.info("Event created with type: {} and status: {}", 
                event.getEventType(), event.getStatus());
            
            Event savedEvent = eventService.saveEvent(event);
            return ResponseEntity.ok(convertToResponse(savedEvent));
        } catch (Exception e) {
            logger.error("Error creating event: ", e);
            throw e;
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<EventResponse>> searchEvents(
            @RequestParam String userId,
            @RequestParam(required = false) List<String> deviceIds,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String pagingState) {
            
        Instant start = startTime != null ? Instant.ofEpochMilli(startTime) : Instant.EPOCH;
        Instant end = endTime != null ? Instant.ofEpochMilli(endTime) : Instant.now();
        
        // Xử lý eventType null
        EventType type = null;
        if (eventType != null && !eventType.isEmpty()) {
            try {
                type = EventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid event type: {}", eventType);
                // Có thể throw exception hoặc xử lý theo logic của bạn
            }
        }

        Slice<Event> events = eventService.searchEvents(
            userId,
            deviceIds,
            type,  // Truyền type đã xử lý
            start,
            end,
            pageSize,
            pagingState
        );

        List<EventResponse> responses = events.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok()
            .header("X-Paging-State", events.hasNext() ? pagingState : null)
            .body(responses);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable String messageId) {
        try {
            logger.info("Getting event for messageId: {}", messageId);
            
            Event event = eventService.getEventByMessageId(messageId);
            
            if (event == null) {
                logger.warn("Event not found for messageId: {}", messageId);
                return ResponseEntity.notFound().build();
            }
            
            // Kiểm tra kỹ các trường bắt buộc
            if (event.getKey() == null) {
                logger.error("Event found but has invalid data structure for messageId: {}", messageId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            try {
                EventResponse response = convertToResponse(event);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                logger.error("Error converting event to response for messageId: {}", messageId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
        } catch (Exception e) {
            logger.error("Error processing request for messageId: {}", messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private EventResponse convertToResponse(Event event) {
        if (event == null || event.getKey() == null) {
            throw new IllegalArgumentException("Event or EventKey cannot be null");
        }
        
        // Đảm bảo các giá trị không null trước khi chuyển đổi
        String status = event.getStatus() != null ? event.getStatus().toString() : "UNKNOWN";
        String eventType = event.getEventType() != null ? event.getEventType().toString() : "UNKNOWN";
        String createdAt = event.getCreatedAt() != null ? event.getCreatedAt().toString() : "";
        
        return new EventResponse(
            event.getKey().getMessageId(),
            event.getKey().getUserId(),
            event.getKey().getDeviceId(),
            eventType,
            event.getDescription(),
            event.getThumbnailUrl(),
            status,
            createdAt
        );
    }
} 