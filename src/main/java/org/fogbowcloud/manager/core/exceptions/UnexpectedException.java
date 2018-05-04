package org.fogbowcloud.manager.core.exceptions;

public class UnexpectedException extends Exception {

	private static final long serialVersionUID = 1L;
	private static final String msg = "Unexpected Exception";
	
	public UnexpectedException() {
		super(msg);
	}
		
	public UnexpectedException(String msg){
		super(msg);
	}
}
