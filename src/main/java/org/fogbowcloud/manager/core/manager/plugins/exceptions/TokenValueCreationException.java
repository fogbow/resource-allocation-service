package org.fogbowcloud.manager.core.manager.plugins.exceptions;

public class TokenValueCreationException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final String message = "Token Creation Exception";

    public TokenValueCreationException() {
        super(message);
    }

    public TokenValueCreationException(String message) {
        super(message);
    }

    public TokenValueCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
