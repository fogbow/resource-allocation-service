package org.fogbowcloud.ras.core.exceptions;

public class InstanceNotFoundException extends FogbowRasException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Instance not found exception";

    public InstanceNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public InstanceNotFoundException(String message) {
        super(message);
    }

    public InstanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
