package com.abcgreen.evcharging.exception;

/**
 * Thrown when a requested record does not exist in the database.
 */
public class RecordNotFoundException extends EVChargingException {
    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String message) {
        super(message);
    }

    public RecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
