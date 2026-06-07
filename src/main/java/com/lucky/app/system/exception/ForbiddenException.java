package com.lucky.app.system.exception;

/** Thrown when an authenticated user attempts an action they are not allowed to perform; maps to HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
