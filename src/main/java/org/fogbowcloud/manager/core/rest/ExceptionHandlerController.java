package org.fogbowcloud.manager.core.rest;

import org.fogbowcloud.manager.core.exceptions.TokenCreationException;
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
	public final ResponseEntity<ExceptionResponse> handleUnauthorizedException(UnauthorizedException ex,
			WebRequest request) {

		ExceptionResponse errorDetails = new ExceptionResponse(ex.getMessage(), request.getDescription(false),
				HttpStatus.UNAUTHORIZED);
		
		return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
	}

	@ExceptionHandler(TokenCreationException.class)
	public final ResponseEntity<ExceptionResponse> handleTokenCreationException(TokenCreationException ex,
			WebRequest request) {

		ExceptionResponse errorDetails = new ExceptionResponse(ex.getMessage(), request.getDescription(false),
				HttpStatus.BAD_REQUEST);

		return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
	}

	@ExceptionHandler(Exception.class)
	public final ResponseEntity<ExceptionResponse> handleAnyException(Exception ex, WebRequest request) {

		ExceptionResponse errorDetails = new ExceptionResponse(ex.getMessage(), request.getDescription(false),
				HttpStatus.BAD_REQUEST);

		return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
	}
}
