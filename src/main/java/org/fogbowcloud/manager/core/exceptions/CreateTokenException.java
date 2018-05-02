package org.fogbowcloud.manager.core.exceptions;

public class CreateTokenException extends Exception {

	private static final long serialVersionUID = 1L;
	private static final String msg = "Error while creating token.";
	
	public CreateTokenException () {
		super(msg);
	}
	
	public CreateTokenException (String msg) {
		super(msg);
	}

}
