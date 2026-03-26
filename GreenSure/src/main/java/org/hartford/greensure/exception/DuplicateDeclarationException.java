package org.hartford.greensure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateDeclarationException extends RuntimeException {
    public DuplicateDeclarationException(String message) { super(message); }
}
