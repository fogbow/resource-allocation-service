package org.fogbowcloud.manager.api.http;

import org.fogbowcloud.manager.core.exceptions.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UnauthorizedRequestException.class)
    public final ResponseEntity<ExceptionResponse> handleAuthorizationException(Exception ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.FORBIDDEN);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(UnauthenticatedUserException.class)
    public final ResponseEntity<ExceptionResponse> handleAuthenticationException(Exception ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(InvalidParameterException.class)
    public final ResponseEntity<ExceptionResponse> handleInvalidParameterException(
            TokenCreationException ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(InstanceNotFoundException.class)
    public final ResponseEntity<ExceptionResponse> handleInstanceNotFoundException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(QuotaExceededException.class)
    public final ResponseEntity<ExceptionResponse> handleQuotaExceededException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                ex.getMessage(), request.getDescription(false), HttpStatus.CONFLICT);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(NoAvailableResourcesException.class)
    public final ResponseEntity<ExceptionResponse> handleNoAvailableResourcesException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                ex.getMessage(), request.getDescription(false), HttpStatus.NOT_ACCEPTABLE);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(UnauthorizedRequestException.class)
    public final ResponseEntity<ExceptionResponse> handleUnavailableProviderException(
            Exception ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                ex.getMessage(), request.getDescription(false), HttpStatus.GATEWAY_TIMEOUT);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @Override
    public final ResponseEntity<Object> handleServletRequestBindingException(
    		ServletRequestBindingException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAnyException(Exception ex, WebRequest request) {

        ExceptionResponse errorDetails = new ExceptionResponse(
                        ex.getMessage(), request.getDescription(false), HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        return new ResponseEntity<>(errorDetails, errorDetails.getStatusCode());
    }
}
