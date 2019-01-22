package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class InvalidTokenException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public InvalidTokenException() {
        super(Messages.Exception.INVALID_TOKEN);
    }

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }

}
