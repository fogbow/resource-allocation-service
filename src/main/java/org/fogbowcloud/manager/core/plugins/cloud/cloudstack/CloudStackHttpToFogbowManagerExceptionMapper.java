package org.fogbowcloud.manager.core.plugins.cloud.cloudstack;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.exceptions.*;

public class CloudStackHttpToFogbowManagerExceptionMapper {

    public static void map(HttpResponseException e)
            throws FogbowManagerException {
        switch(e.getStatusCode()) {
            case HttpStatus.SC_FORBIDDEN:
                throw new UnauthorizedRequestException(e.getMessage(), e);
            case HttpStatus.SC_UNAUTHORIZED:
                throw new UnauthenticatedUserException(e.getMessage(), e);
            case HttpStatus.SC_NOT_FOUND:
                throw new InstanceNotFoundException(e.getMessage(), e);
            default:
                if (e.getStatusCode() > 204) {
                    throw new InvalidParameterException();
                }
        }
    }
}
