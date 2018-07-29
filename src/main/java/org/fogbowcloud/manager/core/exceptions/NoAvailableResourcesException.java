package org.fogbowcloud.manager.core.exceptions;

public class NoAvailableResourcesException extends FogbowManagerException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "No available resources exception";

    public NoAvailableResourcesException() {
        super(DEFAULT_MESSAGE);
    }

    public NoAvailableResourcesException(String message) {
        super(message);
    }

    public NoAvailableResourcesException(String message, Throwable cause) {
        super(message, cause);
    }
}
