package org.fogbowcloud.manager.core.rest;

import org.springframework.http.HttpStatus;

public class ExceptionResponse {

	private String message;
	private String details;
	private HttpStatus statusCode;
	
	public ExceptionResponse(String message, String details, HttpStatus statusCode) {
		this.setMessage(message);
		this.setDetails(details);
		this.setStatusCode(statusCode);
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public HttpStatus getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

}
