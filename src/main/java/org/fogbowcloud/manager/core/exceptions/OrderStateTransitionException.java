package org.fogbowcloud.manager.core.exceptions;

/**
 * Created by arnett on 26/04/18.
 */
public class OrderStateTransitionException extends Exception {

    private final String message;

    public OrderStateTransitionException(String message) {
        this.message = message;
    }
}
