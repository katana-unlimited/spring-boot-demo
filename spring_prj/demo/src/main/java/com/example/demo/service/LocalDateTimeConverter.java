package com.example.demo.service;

import java.time.LocalDateTime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime,  java.sql.Timestamp> {

    @Override
    public java.sql.Timestamp convertToDatabaseColumn(LocalDateTime localDateTime) {
        return (localDateTime == null ? null : java.sql.Timestamp.valueOf(localDateTime));
    }

    @Override
    public LocalDateTime convertToEntityAttribute(java.sql.Timestamp  timestamp) {
        return (timestamp == null ? null : timestamp.toLocalDateTime());
    }
}