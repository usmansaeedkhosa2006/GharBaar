package com.gharbaar.bl;

import java.util.List;

/**
 * [BL] BusinessException
 * Thrown when user input or a business rule is violated
 * (e.g. "Username already exists", "date must be in the future").
 * The UI catches it and shows the messages to the user.
 */
public class BusinessException extends RuntimeException {

    private final List<String> errors;

    public BusinessException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public BusinessException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() { return errors; }
}
