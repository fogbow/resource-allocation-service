package org.fogbowcloud.ras.core.exceptions;

public class FogbowRasException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Fogbow RAS exception";

    public FogbowRasException() {
        super(DEFAULT_MESSAGE);
    }

    public FogbowRasException(String message) {
        super(message);
    }

    public FogbowRasException(String message, Throwable cause) {
        super(message, cause);
    }
}
