package org.hartford.greensure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a file-based operation (upload, save, delete) fails.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FileProcessingException extends RuntimeException {
    public FileProcessingException(String message) {
        super(message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
