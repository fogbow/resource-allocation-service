package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class UnavailableProviderException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public UnavailableProviderException() {
        super(Messages.Exception.UNAVAILABLE_PROVIDER);
    }

    public UnavailableProviderException(String message) {
        super(message);
    }

    public UnavailableProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
