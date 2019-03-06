package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;

public interface GenericRequestPlugin<V extends FogbowGenericRequest, T extends CloudUser> {
    GenericRequestResponse redirectGenericRequest(V genericRequest, T cloudUser) throws FogbowException;

}
