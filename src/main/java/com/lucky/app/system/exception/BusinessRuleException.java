package com.lucky.app.system.exception;

/** Thrown when a domain rule is violated (e.g. inactive customer, reading order); maps to HTTP 400/422. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
