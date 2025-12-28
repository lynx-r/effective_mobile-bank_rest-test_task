package com.example.common.auth.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

public record RequestBlockCardEvent(Long cardId, Long ownerId,
    @JsonSerialize(using = LocalDateTimeSerializer.class) @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime requestedAt) {

}
