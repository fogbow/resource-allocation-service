package org.fogbowcloud.manager.core.models.exceptions;

import org.fogbowcloud.manager.core.models.ResponseConstants;
import org.springframework.http.HttpStatus;

public class RequestException extends Exception {

	public RequestException() {}
	
	public RequestException(HttpStatus httpStatus, String response) {
		super(httpStatus + ": " + response);
	}
}
