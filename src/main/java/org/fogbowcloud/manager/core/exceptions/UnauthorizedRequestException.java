package org.fogbowcloud.manager.core.exceptions;

public class UnauthorizedRequestException extends FogbowManagerException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Unauthorized Error";

    public UnauthorizedRequestException() {
        super(DEFAULT_MESSAGE);
    }

    public UnauthorizedRequestException(String message) {
        super(message);
    }

    public UnauthorizedRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
