package com.camai.fall_fire_detection.service;

import com.camai.fall_fire_detection.config.TestHazelcastConfig;
import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventKey;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventStatus;
import com.camai.fall_fire_detection.repository.EventRepository;
import com.camai.fall_fire_detection.repository.CustomEventRepository;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestHazelcastConfig.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CustomEventRepository customEventRepository;

    @Mock
    private HazelcastInstance hazelcastInstance;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(eventRepository, customEventRepository, hazelcastInstance);
    }

    @Test
    void saveEvent_ValidEvent_ReturnsSavedEvent() {
        // Arrange
        Event event = createTestEvent();
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // Act
        Event savedEvent = eventService.saveEvent(event);

        // Assert
        assertNotNull(savedEvent);
        assertEquals(event.getKey().getMessageId(), savedEvent.getKey().getMessageId());
    }

    @Test
    void saveEvent_NullDeviceId_ThrowsIllegalArgumentException() {
        // Arrange
        Event event = createTestEvent();
        event.setDeviceId(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> eventService.saveEvent(event));
    }

    @Test
    void searchEvents_ValidParameters_ReturnsEventSlice() {
        // Arrange
        String userId = "user1";
        List<String> deviceIds = Arrays.asList("device1");
        EventType eventType = EventType.FALL;
        Instant startTime = Instant.now().minusSeconds(3600);
        Instant endTime = Instant.now();
        int pageSize = 20;
        String pagingState = null;

        List<Event> events = Arrays.asList(createTestEvent());
        Slice<Event> expectedSlice = new SliceImpl<>(events);

        when(customEventRepository.searchEvents(
            userId, deviceIds, eventType, startTime, endTime, pageSize, pagingState
        )).thenReturn(expectedSlice);

        // Act
        Slice<Event> result = eventService.searchEvents(
            userId, deviceIds, eventType, startTime, endTime, pageSize, pagingState
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventType()).isEqualTo(EventType.FALL);
    }

    @Test
    void getEventByMessageId_ExistingMessage_ReturnsEvent() {
        // Arrange
        String messageId = "msg123";
        Event expectedEvent = createTestEvent();
        when(eventRepository.findByMessageId(messageId)).thenReturn(expectedEvent);

        // Act
        Event result = eventService.getEventByMessageId(messageId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo(messageId);
    }

    private Event createTestEvent() {
        Event event = new Event();
        EventKey key = new EventKey();
        
        key.setUserId("user1");
        key.setMessageId("msg123");
        key.setDeviceId("device1");
        key.setEventType(EventType.FALL);
        key.setEventDate(Instant.now());
        key.setBucket(1);
        key.setCategory(1);
        
        event.setKey(key);
        event.setDeviceId("device1");
        event.setEventType(EventType.FALL);
        event.setDescription("Test event");
        event.setStatus(EventStatus.PENDING);
        event.setMessageId("msg123");
        event.setCreatedAt(Instant.now());
        
        return event;
    }
} 