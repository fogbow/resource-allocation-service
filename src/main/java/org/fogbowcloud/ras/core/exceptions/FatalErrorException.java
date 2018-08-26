package org.fogbowcloud.ras.core.exceptions;

public class FatalErrorException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Fatal error exception";

    public FatalErrorException() {
        super(DEFAULT_MESSAGE);
    }

    public FatalErrorException(String message) {
        super(message);
    }

    public FatalErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}
