package org.hartford.greensure.exception;

public class PolicyAlreadyPurchasedException extends RuntimeException {
    public PolicyAlreadyPurchasedException(String message) {
        super(message);
    }
}
