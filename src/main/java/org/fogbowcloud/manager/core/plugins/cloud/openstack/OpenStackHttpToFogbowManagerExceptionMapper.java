package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.exceptions.*;

public class OpenStackHttpToFogbowManagerExceptionMapper {

    public static void map(HttpResponseException e)
            throws FogbowManagerException, UnexpectedException {
        switch(e.getStatusCode()) {
            case HttpStatus.SC_FORBIDDEN:
                throw new UnauthorizedRequestException(e.getMessage(), e);
            case HttpStatus.SC_UNAUTHORIZED:
                throw new UnauthenticatedUserException(e.getMessage(), e);
            case HttpStatus.SC_BAD_REQUEST:
                throw new InstanceNotFoundException(e.getMessage(), e);
            case HttpStatus.SC_NOT_FOUND:
                throw new InstanceNotFoundException(e.getMessage(), e);
            case HttpStatus.SC_CONFLICT:
                throw new QuotaExceededException(e.getMessage(), e);
            case HttpStatus.SC_NOT_ACCEPTABLE:
                throw new NoAvailableResourcesException(e.getMessage(), e);
            case HttpStatus.SC_GATEWAY_TIMEOUT:
                throw new UnavailableProviderException(e.getMessage(), e);
            default:
                throw new UnexpectedException(e.getMessage(), e);
        }
    }
}
