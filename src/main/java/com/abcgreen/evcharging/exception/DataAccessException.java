package com.abcgreen.evcharging.exception;

/**
 * Unchecked wrapper around SQLException so DAO methods stay readable.
 * Raised for database connectivity or SQL errors.
 */
public class DataAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
