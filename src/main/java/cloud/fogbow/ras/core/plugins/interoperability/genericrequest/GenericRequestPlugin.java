package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;

public interface GenericRequestPlugin {

    GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, CloudToken token) throws FogbowException;

}
