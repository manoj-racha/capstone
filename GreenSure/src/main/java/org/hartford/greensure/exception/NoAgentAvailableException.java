package org.hartford.greensure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class NoAgentAvailableException extends RuntimeException {
    public NoAgentAvailableException(String message) { super(message); }
}
