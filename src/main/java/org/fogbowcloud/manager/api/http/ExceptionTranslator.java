package org.fogbowcloud.manager.api.http;

import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    @ExceptionHandler(
    		{UnauthorizedException.class, UnauthenticatedException.class})
    public final ResponseEntity<ExceptionResponse> handleAAException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails =
                new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(TokenCreationException.class)
    public final ResponseEntity<ExceptionResponse> handleTokenCreationException(
            TokenCreationException ex, WebRequest request) {

        ExceptionResponse errorDetails =
                new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(InstanceNotFoundException.class)
    public final ResponseEntity<ExceptionResponse> handleInstanceNotFoundException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails =
                new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAnyException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails =
                new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }
}
