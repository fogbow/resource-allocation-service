package org.fogbowcloud.ras.core.exceptions;

public class UnexpectedException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Unexpected exception";

    public UnexpectedException() {
        super(DEFAULT_MESSAGE);
    }

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
