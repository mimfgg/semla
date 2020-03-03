package io.semla.exception;

public class SemlaException extends RuntimeException {

    public SemlaException(String message) {
        super(message);
    }

    public SemlaException(String message, Throwable cause) {
        super(message, cause);
    }
}
