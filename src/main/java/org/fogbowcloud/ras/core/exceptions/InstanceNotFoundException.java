package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class InstanceNotFoundException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public InstanceNotFoundException() {
        super(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    public InstanceNotFoundException(String message) {
        super(message);
    }

    public InstanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
