package com.lucky.app.system.exception;

/** Thrown for malformed or invalid input that isn't caught by bean validation; maps to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
