package org.fogbowcloud.manager.core.exceptions;

public class UnauthorizedException extends Exception {

	private static final long serialVersionUID = 1L;
	private static final String msg = "Unauthorized Error";
	
	public UnauthorizedException() {
		super(msg);
	}
	
	public UnauthorizedException(String msg) {
		super(msg);
	}
}
