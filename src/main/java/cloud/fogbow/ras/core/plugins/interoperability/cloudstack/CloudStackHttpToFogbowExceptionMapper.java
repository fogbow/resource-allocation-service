package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

public class CloudStackHttpToFogbowExceptionMapper {

    public static void map(HttpResponseException e) throws FogbowException {
        switch (e.getStatusCode()) {
            case HttpStatus.SC_FORBIDDEN:
                throw new UnauthorizedRequestException(e.getMessage(), e);
            case HttpStatus.SC_UNAUTHORIZED:
                throw new UnauthenticatedUserException(e.getMessage(), e);
            case HttpStatus.SC_NOT_FOUND:
                throw new InstanceNotFoundException(e.getMessage(), e);
            default:
                if (e.getStatusCode() > 204) {
                    throw new FogbowException(e.getMessage(), e);
                }
        }
    }
}
