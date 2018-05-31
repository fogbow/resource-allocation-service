package org.fogbowcloud.manager.core.manager.plugins.exceptions;

import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;

public class InvalidCredentialsException extends UnauthenticatedException {

    private static final long serialVersionUID = 1L;
    private static final String message = "Invalid Credentials";

    public InvalidCredentialsException() {
        super(message);
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
