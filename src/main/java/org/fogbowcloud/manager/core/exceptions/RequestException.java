package org.fogbowcloud.manager.core.exceptions;

import org.fogbowcloud.manager.core.models.ErrorType;

public class RequestException extends Exception {

    public RequestException() {}

    public RequestException(ErrorType httpStatus, String response) {
        super(httpStatus.toString() + ": " + response);
    }
}
