package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class FatalErrorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FatalErrorException() {
        super(Messages.Exception.FATAL_ERROR);
    }

    public FatalErrorException(String message) {
        super(message);
    }

    public FatalErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}
