package com.abcgreen.evcharging.exception;

/**
 * Thrown when login fails or the user does not have permission for an action.
 */
public class AuthenticationException extends EVChargingException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
