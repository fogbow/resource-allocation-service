package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class UnauthenticatedUserException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public UnauthenticatedUserException() {
        super(Messages.Exception.AUTHENTICATION_ERROR);
    }

    public UnauthenticatedUserException(String message) {
        super(message);
    }

    public UnauthenticatedUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
