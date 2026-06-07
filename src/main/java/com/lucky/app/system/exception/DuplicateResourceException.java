package com.lucky.app.system.exception;

/** Thrown when creating a resource that violates a uniqueness rule (email, national ID); maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
