package org.fogbowcloud.ras.core.exceptions;

public class UnavailableProviderException extends FogbowRasException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Unavailable provider exception";

    public UnavailableProviderException() {
        super(DEFAULT_MESSAGE);
    }

    public UnavailableProviderException(String message) {
        super(message);
    }

    public UnavailableProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
