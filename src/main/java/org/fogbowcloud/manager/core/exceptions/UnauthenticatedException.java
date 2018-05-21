package org.fogbowcloud.manager.core.exceptions;

public class UnauthenticatedException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Unauthenticated Error";

    public UnauthenticatedException() {
        super(DEFAULT_MESSAGE);
    }

    public UnauthenticatedException(String message) {
        super(message);
    }
}
