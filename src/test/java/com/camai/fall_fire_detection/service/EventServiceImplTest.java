package com.camai.fall_fire_detection.service;

import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventKey;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventStatus;
import com.camai.fall_fire_detection.repository.EventRepository;
import com.camai.fall_fire_detection.repository.CustomEventRepository;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CustomEventRepository customEventRepository;

    @Mock
    private HazelcastInstance hazelcastInstance;

    private EventServiceImpl eventService;
    private Map<String, Event> cacheMap;

    @BeforeEach
    void setUp() {
        cacheMap = new ConcurrentHashMap<>();
        
        @SuppressWarnings("unchecked")
        IMap<String, Event> mockCache = mock(IMap.class, invocation -> {
            String methodName = invocation.getMethod().getName();
            if (methodName.equals("put")) {
                return cacheMap.put((String) invocation.getArgument(0), (Event) invocation.getArgument(1));
            } else if (methodName.equals("get")) {
                return cacheMap.get(invocation.getArgument(0));
            }
            return null;
        });
        
        doReturn(mockCache).when(hazelcastInstance).getMap(anyString());
        eventService = new EventServiceImpl(eventRepository, customEventRepository, hazelcastInstance);
    }

    @Test
    void saveEvent_ShouldSaveAndReturnEvent() {
        // Arrange
        Event event = createTestEvent();
        
        // Act
        Event result = eventService.saveEvent(event);
        
        // Assert
        verify(customEventRepository).saveEventAsync(event);
        assertEquals(event.getMessageId(), result.getMessageId());
        assertEquals(event, cacheMap.get(event.getMessageId()));
    }

    @Test
    void getEventByMessageId_WhenInCache_ShouldReturnFromCache() {
        // Arrange
        Event event = createTestEvent();
        cacheMap.put(event.getMessageId(), event);
        
        // Act
        Event result = eventService.getEventByMessageId(event.getMessageId());
        
        // Assert
        assertEquals(event, result);
        verify(eventRepository, never()).findByMessageId(anyString());
    }

    @Test
    void getEventByMessageId_WhenNotInCache_ShouldFetchFromDB() {
        // Arrange
        Event event = createTestEvent();
        when(eventRepository.findByMessageId(event.getMessageId())).thenReturn(event);
        
        // Act
        Event result = eventService.getEventByMessageId(event.getMessageId());
        
        // Assert
        assertEquals(event, result);
        assertEquals(event, cacheMap.get(event.getMessageId()));
    }

    @Test
    void searchEvents_ShouldReturnSliceOfEvents() {
        // Arrange
        String userId = "user1";
        List<String> deviceIds = Arrays.asList("device1", "device2");
        EventType eventType = EventType.FALL;
        Instant startTime = Instant.now().minusSeconds(3600);
        Instant endTime = Instant.now();
        int pageSize = 20;
        String pagingState = "someState";
        
        List<Event> events = Arrays.asList(createTestEvent(), createTestEvent());
        Slice<Event> expectedSlice = new SliceImpl<>(events);
        
        when(customEventRepository.searchEvents(
            userId, deviceIds, eventType, startTime, endTime, pageSize, pagingState
        )).thenReturn(expectedSlice);
        
        // Act
        Slice<Event> result = eventService.searchEvents(
            userId, deviceIds, eventType, startTime, endTime, pageSize, pagingState
        );
        
        // Assert
        assertEquals(expectedSlice, result);
    }

    private Event createTestEvent() {
        Event event = new Event();
        EventKey key = new EventKey();
        
        key.setUserId("user1");
        key.setBucket(1);
        key.setEventDate(Instant.now());
        key.setDeviceId("device1");
        key.setEventType(EventType.FALL);
        key.setCategory(1);
        key.setMessageId("msg1");
        
        event.setKey(key);
        event.setMessageId("msg1");
        event.setDeviceId("device1");
        event.setEventType(EventType.FALL);
        event.setDescription("Test event");
        event.setThumbnailUrl("http://example.com/thumb.jpg");
        event.setStatus(EventStatus.PENDING);
        event.setCreatedAt(Instant.now());
        
        return event;
    }
} 