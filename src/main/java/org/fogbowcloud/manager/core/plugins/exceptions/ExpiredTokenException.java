package org.fogbowcloud.manager.core.plugins.exceptions;

import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;

public class ExpiredTokenException extends Exception {

    private static final long serialVersionUID = 1L;
    private static final String message = "Expired token.";

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
