package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;

public interface GenericRequestPlugin<V extends FogbowGenericRequest, T extends CloudUser> {
    FogbowGenericResponse redirectGenericRequest(V genericRequest, T cloudUser) throws FogbowException;

}
