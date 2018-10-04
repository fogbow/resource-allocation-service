package org.fogbowcloud.ras.core.exceptions;

import org.fogbowcloud.ras.core.constants.Messages;

public class QuotaExceededException extends FogbowRasException {
    private static final long serialVersionUID = 1L;

    public QuotaExceededException() {
        super(Messages.Exception.QUOTA_EXCEEDED);
    }

    public QuotaExceededException(String message) {
        super(message);
    }

    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
