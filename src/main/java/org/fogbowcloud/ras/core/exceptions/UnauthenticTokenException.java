package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class UnauthenticTokenException extends UnauthenticatedUserException {
    private static final long serialVersionUID = 1L;

    public UnauthenticTokenException() {
        super(Messages.Exception.AUTHENTICATION_ERROR);
    }

    public UnauthenticTokenException(String message) {
        super(message);
    }

    public UnauthenticTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
