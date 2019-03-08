package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;

public class StubGenericRequestRequestPlugin implements GenericRequestPlugin<CloudUser> {

    public StubGenericRequestRequestPlugin() {
    }

    @Override
    public FogbowGenericResponse redirectGenericRequest(String genericRequest, CloudUser token) {
        return null;
    }

}
