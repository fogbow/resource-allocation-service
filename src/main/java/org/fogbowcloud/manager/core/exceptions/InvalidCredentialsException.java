package org.fogbowcloud.manager.core.exceptions;

public class InvalidCredentialsException extends UnauthorizedException {

	private static final long serialVersionUID = 1L;
	private static final String msg = "Invalid Credentials";
	
	public InvalidCredentialsException() {
		super(msg);
	}
	
	public InvalidCredentialsException (String msg) {
		super(msg);
	}
	
}
