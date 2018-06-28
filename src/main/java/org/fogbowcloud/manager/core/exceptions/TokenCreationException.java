package org.fogbowcloud.manager.core.exceptions;

public class TokenCreationException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final String message = "Token Creation Exception";

    public TokenCreationException() {
        super(message);
    }

    public TokenCreationException(String message) {
        super(message);
    }

    public TokenCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
