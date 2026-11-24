package com.gharbaar.db;

/**
 * [DB LAYER] DataAccessException
 * Wraps every SQLException so the UI never sees raw JDBC errors
 * (exception handling requirement of the rubric).
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
