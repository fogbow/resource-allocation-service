package org.fogbowcloud.manager.core.exceptions;

import org.fogbowcloud.manager.core.plugins.cloud.models.ErrorType;

public class FogbowManagerException extends Exception {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Fogbow Manager exception";

    public FogbowManagerException() {
        super(DEFAULT_MESSAGE);
    }

    public FogbowManagerException(String message) {
        super(message);
    }

    public FogbowManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public FogbowManagerException(ErrorType httpStatus, String response) {
        super(httpStatus.toString() + ": " + response);
    }
}
