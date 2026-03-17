package org.hartford.greensure.exception;

public class DuplicateAgentFieldException extends RuntimeException {

    private final String field;

    public DuplicateAgentFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
