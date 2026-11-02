package com.abcgreen.evcharging.exception;

/**
 * Thrown when user input fails validation (empty fields, bad email, invalid registration number...).
 */
public class ValidationException extends EVChargingException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
