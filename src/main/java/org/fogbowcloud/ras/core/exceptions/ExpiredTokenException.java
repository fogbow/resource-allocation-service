package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class ExpiredTokenException extends UnauthenticatedUserException {
    private static final long serialVersionUID = 1L;

    public ExpiredTokenException() {
        super(Messages.Exception.EXPIRED_TOKEN);
    }

    public ExpiredTokenException(String message) {
        super(message);
    }

    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
