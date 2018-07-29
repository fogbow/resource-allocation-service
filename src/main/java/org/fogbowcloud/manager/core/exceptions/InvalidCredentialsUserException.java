package org.fogbowcloud.manager.core.exceptions;

public class InvalidCredentialsUserException extends UnauthenticatedUserException {
    private static final long serialVersionUID = 1L;
    private static final String message = "Invalid Credentials";

    public InvalidCredentialsUserException() {
        super(message);
    }

    public InvalidCredentialsUserException(String message) {
        super(message);
    }

    public InvalidCredentialsUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
