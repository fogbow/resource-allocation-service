package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.GenericRequest;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;

public interface GenericRequestPlugin<T extends CloudUser> {

    GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, T token) throws FogbowException;

}
