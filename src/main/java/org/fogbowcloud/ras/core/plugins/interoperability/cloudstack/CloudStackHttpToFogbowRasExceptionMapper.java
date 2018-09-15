package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.*;

public class CloudStackHttpToFogbowRasExceptionMapper {

    public static void map(HttpResponseException e)
            throws FogbowRasException {
        switch (e.getStatusCode()) {
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
