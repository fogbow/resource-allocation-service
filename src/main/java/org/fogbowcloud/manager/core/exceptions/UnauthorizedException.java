package org.fogbowcloud.manager.core.exceptions;

public class UnauthorizedException extends Exception {

	private static final long serialVersionUID = 1L;
	private static final String message = "Unauthorized Error";
	
	public UnauthorizedException() {
		super(message);
	}
	
	public UnauthorizedException(String message) {
		super(message);
	}
}
