package com.lucky.app.system.exception;

/** Thrown for failed/absent authentication (bad credentials, unverified email); maps to HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
