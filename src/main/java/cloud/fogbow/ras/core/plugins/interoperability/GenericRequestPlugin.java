package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;

public interface GenericRequestPlugin<T extends CloudUser> {
    FogbowGenericResponse redirectGenericRequest(String genericRequest, T cloudUser) throws FogbowException;

}
