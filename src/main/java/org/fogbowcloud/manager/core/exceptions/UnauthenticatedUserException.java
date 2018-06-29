package org.fogbowcloud.manager.core.exceptions;

public class UnauthenticatedUserException extends FogbowManagerException {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Unauthenticated Error";

    public UnauthenticatedUserException() {
        super(DEFAULT_MESSAGE);
    }

    public UnauthenticatedUserException(String message) {
        super(message);
    }

    public UnauthenticatedUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
