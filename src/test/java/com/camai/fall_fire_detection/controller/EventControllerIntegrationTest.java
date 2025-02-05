package com.camai.fall_fire_detection.controller;

import com.camai.fall_fire_detection.dto.EventRequest;
import com.camai.fall_fire_detection.dto.EventResponse;
import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventKey;
import com.camai.fall_fire_detection.model.EventStatus;
import com.camai.fall_fire_detection.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebMvcTest(EventController.class)
@Import({ObjectMapper.class, JavaTimeModule.class})
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Autowired
    private EventController eventController;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createEvent_ShouldReturnCreatedEvent() {
        // Arrange
        EventRequest request = new EventRequest();
        request.setUserId("user1");
        request.setDeviceId("device1");
        request.setMessageId("msg1");
        request.setEventType("FALL");
        request.setDescription("Fall detected in living room");
        request.setThumbnailUrl("http://example.com/thumbnail.jpg");
        request.setTimestamp(Instant.parse("2024-02-06T10:30:00Z"));

        Event mockEvent = new Event();
        EventKey key = new EventKey();
        key.setMessageId(request.getMessageId());
        key.setUserId(request.getUserId());
        key.setDeviceId(request.getDeviceId());
        mockEvent.setKey(key);
        mockEvent.setEventType(EventType.valueOf(request.getEventType()));
        mockEvent.setStatus(EventStatus.PENDING);
        mockEvent.setDescription(request.getDescription());
        mockEvent.setThumbnailUrl(request.getThumbnailUrl());
        mockEvent.setCreatedAt(request.getTimestamp());
        
        when(eventService.saveEvent(any(Event.class))).thenReturn(mockEvent);

        // Act
        ResponseEntity<EventResponse> response = eventController.createEvent(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        EventResponse eventResponse = response.getBody();
        assertNotNull(eventResponse);
        assertEquals("msg1", eventResponse.getMessageId());
        assertEquals("user1", eventResponse.getUserId());
        assertEquals("device1", eventResponse.getDeviceId());
        assertEquals("FALL", eventResponse.getEventType());
        assertEquals("Fall detected in living room", eventResponse.getDescription());
        assertEquals("http://example.com/thumbnail.jpg", eventResponse.getThumbnailUrl());
        assertEquals("PENDING", eventResponse.getStatus());
        assertEquals("2024-02-06T10:30:00Z", eventResponse.getCreatedAt());
    }

    @Test
    void getEvent_ShouldReturnEvent() throws Exception {
        // Arrange
        Event mockEvent = new Event();
        EventKey key = new EventKey();
        key.setMessageId("msg1");
        key.setUserId("user1");
        key.setDeviceId("device1");
        mockEvent.setKey(key);
        mockEvent.setEventType(EventType.FALL);
        mockEvent.setDescription("Fall detected");
        mockEvent.setThumbnailUrl("http://example.com/thumb.jpg");
        mockEvent.setStatus(EventStatus.PENDING);
        mockEvent.setCreatedAt(Instant.parse("2024-02-06T10:30:00Z"));
        
        when(eventService.getEventByMessageId("msg1")).thenReturn(mockEvent);

        // Act & Assert
        mockMvc.perform(get("/fall-fire/api/v1/events/{messageId}", "msg1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("msg1"))
                .andExpect(jsonPath("$.userId").value("user1"))
                .andExpect(jsonPath("$.deviceId").value("device1"))
                .andExpect(jsonPath("$.eventType").value("FALL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.description").value("Fall detected"))
                .andExpect(jsonPath("$.thumbnailUrl").value("http://example.com/thumb.jpg"))
                .andExpect(jsonPath("$.createdAt").value("2024-02-06T10:30:00Z"));
    }

    @Test
    void searchEvents_ShouldReturnEvents() throws Exception {
        // Arrange
        List<Event> events = Arrays.asList(new Event(), new Event());
        Slice<Event> mockSlice = new SliceImpl<>(events);
        
        when(eventService.searchEvents(
            anyString(), anyList(), any(EventType.class), 
            any(Instant.class), any(Instant.class), 
            anyInt(), anyString()
        )).thenReturn(mockSlice);

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/search")
                .param("userId", "user1")
                .param("deviceIds", "device1,device2")
                .param("eventType", "FALL")
                .param("startTime", String.valueOf(Instant.now().minusSeconds(3600).toEpochMilli()))
                .param("endTime", String.valueOf(Instant.now().toEpochMilli()))
                .param("pageSize", "20"))
                .andExpect(status().isOk());
    }
} 