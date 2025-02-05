package com.camai.fall_fire_detection.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import com.camai.fall_fire_detection.model.EventStatus;
import com.camai.fall_fire_detection.model.EventType;
public class EventTypeConverters {

    @ReadingConverter
    public static class StringToEventTypeConverter implements Converter<String, EventType> {
        @Override
        public EventType convert(String source) {
            return EventType.fromValue(source);
        }
    }

    @WritingConverter
    public static class EventTypeToStringConverter implements Converter<EventType, String> {
        @Override
        public String convert(EventType source) {
            return source != null ? source.getValue() : null;
        }
    }

    @ReadingConverter
public static class StringToEventStatusConverter implements Converter<String, EventStatus> {
    @Override
    public EventStatus convert(String source) {
        return EventStatus.valueOf(source);
    }
}

@WritingConverter
public static class EventStatusToStringConverter implements Converter<EventStatus, String> {
    @Override
        public String convert(EventStatus source) {
            return source != null ? source.name() : null;
        }
    }
}