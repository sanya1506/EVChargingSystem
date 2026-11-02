package com.abcgreen.evcharging.exception;

/**
 * Thrown when a record that must be unique already exists (email, phone, vehicle number, station code...).
 */
public class DuplicateRecordException extends EVChargingException {
    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }

    public DuplicateRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
