package org.fogbowcloud.manager.core.models.exceptions;

public class RequestException extends Exception {

	public RequestException() {}
	
	public RequestException(int httpStatus, String response) {
		super(httpStatus + ": " + response);
	}
}
