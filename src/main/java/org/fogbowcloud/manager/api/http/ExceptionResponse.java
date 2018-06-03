package org.fogbowcloud.manager.api.http;

import org.springframework.http.HttpStatus;

public class ExceptionResponse {

    private String message;
    private String details;
    private HttpStatus statusCode;

    public ExceptionResponse(String message, String details, HttpStatus statusCode) {
        this.message = message;
        this.details = details;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
