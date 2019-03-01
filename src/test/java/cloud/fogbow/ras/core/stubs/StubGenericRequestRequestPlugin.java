package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.common.util.connectivity.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;

public class StubGenericRequestRequestPlugin implements GenericRequestPlugin<CloudToken> {

    public StubGenericRequestRequestPlugin() {
    }

    @Override
    public GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, CloudToken token) {
        return null;
    }

}
