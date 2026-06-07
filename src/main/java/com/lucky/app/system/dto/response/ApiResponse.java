package com.lucky.app.system.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Standard success envelope wrapping every API result: success flag, message, and typed data payload. */
@Getter
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
