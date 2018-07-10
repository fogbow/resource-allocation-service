package org.fogbowcloud.manager.core.exceptions;

public class OrderNotFoundException extends FogbowManagerException {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Order not found exception";

    public OrderNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
