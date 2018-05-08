package org.fogbowcloud.manager.core.models;

public class StatusResponse {

    private ErrorType errorType;
    private String responseConstants;

    public StatusResponse(ErrorType errorType, String responseConstants) {
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
