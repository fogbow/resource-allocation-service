package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class InvalidUserCredentialsException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public InvalidUserCredentialsException() {
        super(Messages.Exception.INVALID_CREDENTIALS);
    }

    public InvalidUserCredentialsException(String message) {
        super(message);
    }

    public InvalidUserCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
