package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class FogbowRasException extends Exception {
    private static final long serialVersionUID = 1L;

    public FogbowRasException() {
        super(Messages.Exception.FOGBOW_RAS);
    }

    public FogbowRasException(String message) {
        super(message);
    }

    public FogbowRasException(String message, Throwable cause) {
        super(message, cause);
    }
}
