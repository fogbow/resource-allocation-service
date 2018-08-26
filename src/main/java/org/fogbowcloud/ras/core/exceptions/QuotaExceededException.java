package org.fogbowcloud.ras.core.exceptions;

public class QuotaExceededException extends FogbowRasException {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Quota exceeded exception";

    public QuotaExceededException() {
        super(DEFAULT_MESSAGE);
    }

    public QuotaExceededException(String message) {
        super(message);
    }

    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
