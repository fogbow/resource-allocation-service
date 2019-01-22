package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class RemoteCommunicationException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public RemoteCommunicationException() {
        super(Messages.Exception.REMOTE_COMMUNICATION);
    }

    public RemoteCommunicationException(String message) {
        super(message);
    }

    public RemoteCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
