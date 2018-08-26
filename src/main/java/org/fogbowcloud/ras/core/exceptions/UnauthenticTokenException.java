package org.fogbowcloud.ras.core.exceptions;

public class UnauthenticTokenException extends UnauthenticatedUserException {
    private static final long serialVersionUID = 1L;
    private static final String message = "Unauthentic tokens exception";

    public UnauthenticTokenException() {
        super(message);
    }

    public UnauthenticTokenException(String message) {
        super(message);
    }

    public UnauthenticTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
