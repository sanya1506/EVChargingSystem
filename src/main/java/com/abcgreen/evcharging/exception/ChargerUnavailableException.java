package com.abcgreen.evcharging.exception;

/**
 * Thrown when no charger is free for the requested station / slot. The caller may offer the waiting queue.
 */
public class ChargerUnavailableException extends EVChargingException {
    private static final long serialVersionUID = 1L;

    public ChargerUnavailableException(String message) {
        super(message);
    }

    public ChargerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
