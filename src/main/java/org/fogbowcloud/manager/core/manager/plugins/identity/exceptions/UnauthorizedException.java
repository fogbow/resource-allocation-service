package org.fogbowcloud.manager.core.manager.plugins.identity.exceptions;

public class UnauthorizedException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_MESSAGE = "Unauthorized Error";
	
	public UnauthorizedException() {
		super(DEFAULT_MESSAGE);
	}
	
	public UnauthorizedException(String message) {
		super(message);
	}
	
	public UnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}
}
