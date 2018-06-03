package org.fogbowcloud.manager.api.http;

import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(
    		{UnauthorizedException.class, UnauthenticatedException.class})
    public final ResponseEntity<ExceptionResponse> handleAAException(
            UnauthorizedException ex, WebRequest request) {

        ExceptionResponse errorDetails =
                new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }
	
    @org.springframework.web.bind.annotation.ExceptionHandler(TokenCreationException.class)
    public final ResponseEntity<ExceptionResponse> handleTokenCreationException(
            TokenCreationException ex, WebRequest request) {

        ExceptionResponse errorDetails =
                new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAnyException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails =
                new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }
}
