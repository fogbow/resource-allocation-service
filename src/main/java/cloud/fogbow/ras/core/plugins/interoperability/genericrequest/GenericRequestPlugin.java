package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;

public interface GenericRequestPlugin<T extends CloudToken> {

    GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, T token) throws FogbowException;

}
