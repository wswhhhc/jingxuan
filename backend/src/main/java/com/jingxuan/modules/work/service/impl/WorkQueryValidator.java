package com.jingxuan.modules.work.service.impl;

import com.jingxuan.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
class WorkQueryValidator {

    private static final DateTimeFormatter QUERY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    void validateDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            LocalDateTime.parse(value, QUERY_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(fieldName + " 日期格式无效，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }
}
