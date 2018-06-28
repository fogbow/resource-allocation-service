package org.fogbowcloud.manager.core.models;

public class ErrorResponse {

    private ErrorType errorType;
    private String responseConstants;

    public ErrorResponse(ErrorType errorType, String responseConstants) {
        this.errorType = errorType;
        this.responseConstants = responseConstants;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getResponseConstants() {
        return responseConstants;
    }
}
