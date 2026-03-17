package org.hartford.greensure.exception;

public class MaxResubmissionLimitExceededException extends RuntimeException {
    public MaxResubmissionLimitExceededException(String message) {
        super(message);
    }
}
