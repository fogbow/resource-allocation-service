package org.fogbowcloud.manager.core.manager.plugins.identity.exceptions;

public class InvalidTokenException extends UnauthorizedException {

    private static final long serialVersionUID = 1L;
    private static final String message = "Invalid token";

    public InvalidTokenException() {
        super(message);
    }

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
