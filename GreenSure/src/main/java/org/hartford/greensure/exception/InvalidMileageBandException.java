package org.hartford.greensure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMileageBandException extends RuntimeException {
    public InvalidMileageBandException(String message) { super(message); }
}
