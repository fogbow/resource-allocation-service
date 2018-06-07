package org.fogbowcloud.manager.core.exceptions;

public class ImageException extends Exception {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Image Exception";

    public ImageException() {
        super(DEFAULT_MESSAGE);
    }

    public ImageException(String message) {
        super(message);
    }

    public ImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
