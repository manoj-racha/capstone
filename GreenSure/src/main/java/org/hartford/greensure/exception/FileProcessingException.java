package org.hartford.greensure.exception;

/**
 * Thrown when a file-based operation (upload, save, delete) fails.
 */
public class FileProcessingException extends RuntimeException {
    public FileProcessingException(String message) {
        super(message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
