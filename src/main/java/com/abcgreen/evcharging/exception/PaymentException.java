package com.abcgreen.evcharging.exception;

/**
 * Thrown when a (simulated) payment fails or the bill is already settled.
 */
public class PaymentException extends EVChargingException {
    private static final long serialVersionUID = 1L;

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
