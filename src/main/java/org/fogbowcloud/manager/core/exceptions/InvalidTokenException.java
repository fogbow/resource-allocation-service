package org.fogbowcloud.manager.core.exceptions;

public class InvalidTokenException extends UnauthorizedException {

	private static final long serialVersionUID = 1L;
	private static final String msg = "Invalid token";
	
	public InvalidTokenException() {
		super(msg);
	}
		
	public InvalidTokenException(String msg){
		super(msg);
	}
}
