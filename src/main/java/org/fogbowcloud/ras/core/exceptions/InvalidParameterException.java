package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class InvalidParameterException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public InvalidParameterException() {
        super(Messages.Exception.INVALID_PARAMETER);
    }

    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

}
