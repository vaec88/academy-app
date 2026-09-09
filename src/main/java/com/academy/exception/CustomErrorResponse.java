package com.academy.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomErrorResponse(
        LocalDateTime dateTime,
        int status,
        String message,
        String path,
        List<ValidationError> errors
) {

    public CustomErrorResponse(int status, String message, String path) {
        this(LocalDateTime.now(), status, message, path, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ValidationError(String field, String message) {
    }
}
