package org.fogbowcloud.manager.core.exceptions;

/**
 * Created by arnett on 26/04/18.
 */
public class OrderStateTransitionException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    public OrderStateTransitionException(String message) {
    	super(message);
    }
}
