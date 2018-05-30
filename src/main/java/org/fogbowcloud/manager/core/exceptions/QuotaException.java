package org.fogbowcloud.manager.core.exceptions;

public class QuotaException extends Exception {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Quota Exception";

    public QuotaException() {
        super(DEFAULT_MESSAGE);
    }

    public QuotaException(String message) {
        super(message);
    }

    public QuotaException(String message, Throwable cause) {
        super(message, cause);
    }
}
