package org.fogbowcloud.ras.core.exceptions;

public class ExpiredTokenException extends UnauthenticatedUserException {
    private static final long serialVersionUID = 1L;
    private static final String message = "Expired tokens exception";

    public ExpiredTokenException() {
        super(message);
    }

    public ExpiredTokenException(String message) {
        super(message);
    }

    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
