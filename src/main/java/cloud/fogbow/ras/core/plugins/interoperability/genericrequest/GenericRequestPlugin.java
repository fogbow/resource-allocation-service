package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;

public interface GenericRequestPlugin<T extends CloudToken, V extends GenericRequest> {

    GenericRequestResponse redirectGenericRequest(V genericRequest, T token) throws FogbowException;

}
