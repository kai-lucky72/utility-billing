package com.lucky.app.system.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Standard error envelope returned by the global exception handler: flag, message, timestamp, and request path. */
@Getter
@Builder
public class ErrorResponse {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    private String path;
}
