package org.fogbowcloud.manager.core.rest;

import java.rmi.UnexpectedException;

import org.fogbowcloud.manager.core.exceptions.InvalidCredentialsException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerController extends ResponseEntityExceptionHandler {

	@ExceptionHandler(UnauthorizedException.class)
	public final ResponseEntity<ExceptionResponse> handleUnauthorizedException(InvalidCredentialsException ex,
			WebRequest request) {
		
		ExceptionResponse errorDetails = new ExceptionResponse();
		errorDetails.setDetails(request.getDescription(false));
		errorDetails.setMessage(ex.getMessage());
		errorDetails.setStatusCode(HttpStatus.UNAUTHORIZED);
		
		return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
	}
	
	@ExceptionHandler(UnexpectedException.class)
	public final ResponseEntity<ExceptionResponse> handleUnexpectedException(InvalidCredentialsException ex,
			WebRequest request) {
		
		ExceptionResponse errorDetails = new ExceptionResponse();
		errorDetails.setDetails(request.getDescription(false));
		errorDetails.setMessage(ex.getMessage());
		errorDetails.setStatusCode(HttpStatus.BAD_REQUEST);
		
		return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
	}

}
