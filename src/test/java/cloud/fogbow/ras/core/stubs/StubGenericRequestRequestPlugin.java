package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.FogbowGenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;

public class StubGenericRequestRequestPlugin implements GenericRequestPlugin<FogbowGenericRequest, CloudUser> {

    public StubGenericRequestRequestPlugin() {
    }

    @Override
    public GenericRequestResponse redirectGenericRequest(FogbowGenericRequest genericRequest, CloudUser token) {
        return null;
    }

}
