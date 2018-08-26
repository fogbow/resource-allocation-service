package org.fogbowcloud.ras.core.exceptions;

public class InvalidUserCredentialsException extends FogbowRasException {
    private static final long serialVersionUID = 1L;
    private static final String message = "Invalid Credentials";

    public InvalidUserCredentialsException() {
        super(message);
    }

    public InvalidUserCredentialsException(String message) {
        super(message);
    }

    public InvalidUserCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
