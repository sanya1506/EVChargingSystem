package com.abcgreen.evcharging.exception;

/**
 * Base checked exception for all business errors in the EV charging system.
 * Every custom exception in the application extends this class, so callers can
 * catch a single type when they only need to show the message to the user.
 */
public class EVChargingException extends Exception {
    private static final long serialVersionUID = 1L;

    public EVChargingException(String message) {
        super(message);
    }

    public EVChargingException(String message, Throwable cause) {
        super(message, cause);
    }
}
