package com.camai.fall_fire_detection.integration;

import com.camai.fall_fire_detection.config.TestHazelcastConfig;
import com.camai.fall_fire_detection.dto.EventRequest;
import com.camai.fall_fire_detection.dto.EventResponse;
import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestHazelcastConfig.class)
public class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void createEvent_ValidRequest_ReturnsCreatedEvent() throws Exception {
        // Arrange
        EventRequest request = createTestEventRequest();
        String requestJson = objectMapper.writeValueAsString(request);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(request.getMessageId()))
                .andExpect(jsonPath("$.userId").value(request.getUserId()))
                .andExpect(jsonPath("$.eventType").value(request.getEventType()))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        EventResponse response = objectMapper.readValue(responseJson, EventResponse.class);
        assertThat(response).isNotNull();
    }

    @Test
    void searchEvents_ValidParameters_ReturnsEventList() throws Exception {
        // Arrange
        String userId = "testUser";
        String deviceId = "testDevice";
        String eventType = EventType.FALL.toString();

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/search")
                .param("userId", userId)
                .param("deviceIds", deviceId)
                .param("eventType", eventType)
                .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getEvent_ExistingMessageId_ReturnsEvent() throws Exception {
        // Arrange
        String messageId = "testMessage123";
        Event savedEvent = saveTestEvent(messageId);

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/{messageId}", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId));
    }

    private EventRequest createTestEventRequest() {
        EventRequest request = new EventRequest();
        request.setUserId("testUser");
        request.setMessageId("testMessage");
        request.setDeviceId("testDevice");
        request.setEventType(EventType.FALL.toString());
        request.setDescription("Test Description");
        request.setTimestamp(Instant.now());
        return request;
    }

    private Event saveTestEvent(String messageId) {
        // Implement logic to save a test event to the database
        // This will depend on your actual Event entity structure
        return null; // Replace with actual implementation
    }
} 