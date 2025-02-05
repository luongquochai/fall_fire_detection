package com.camai.fall_fire_detection.config;

import com.camai.fall_fire_detection.model.EventType;
import com.datastax.oss.driver.api.core.type.codec.MappingCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;

public class EventTypeCodec extends MappingCodec<String, EventType> {
    
    public EventTypeCodec() {
        super(TypeCodecs.TEXT, GenericType.of(EventType.class));
    }

    @Override
    protected EventType innerToOuter(String value) {
        return value != null ? EventType.fromValue(value) : EventType.UNKNOWN;
    }

    @Override
    protected String outerToInner(EventType value) {
        return value != null ? value.getValue() : EventType.UNKNOWN.getValue();
    }
} 