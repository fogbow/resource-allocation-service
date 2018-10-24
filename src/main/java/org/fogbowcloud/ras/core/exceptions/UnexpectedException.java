package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class UnexpectedException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnexpectedException() {
        super(Messages.Exception.UNEXPECTED_ERROR);
    }

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
