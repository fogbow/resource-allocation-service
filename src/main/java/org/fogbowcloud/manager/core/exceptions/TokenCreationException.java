package org.fogbowcloud.manager.core.exceptions;

public class TokenCreationException extends Exception {

	private static final long serialVersionUID = 1L;
	private static final String msg = "Token Creation Exception"; 
	
	public TokenCreationException() {
		super(msg);
	}
	
	public TokenCreationException(String msg) {
		super(msg);
	}
}
