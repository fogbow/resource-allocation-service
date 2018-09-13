package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class UnauthorizedRequestException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedRequestException() {
        super(Messages.Exception.UNAUTHORIZED_ERROR);
    }

    public UnauthorizedRequestException(String message) {
        super(message);
    }

    public UnauthorizedRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
