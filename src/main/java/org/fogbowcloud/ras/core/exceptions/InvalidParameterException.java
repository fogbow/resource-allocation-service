package org.fogbowcloud.ras.core.exceptions;

public class InvalidParameterException extends FogbowRasException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Invalid parameter exception";

    public InvalidParameterException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

}
