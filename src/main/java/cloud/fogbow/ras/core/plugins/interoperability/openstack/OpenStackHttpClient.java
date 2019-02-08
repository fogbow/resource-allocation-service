package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.util.connectivity.CloudHttpClient;
import org.apache.http.client.HttpClient;

public class OpenStackHttpClient extends CloudHttpClient {
    public OpenStackHttpClient(Integer timeout) throws FatalErrorException {
        super(timeout);
    }

    public OpenStackHttpClient(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public GenericRequest includeTokenInRequest(GenericRequest genericRequest, CloudToken token) {
        // TODO ARNETT
        return null;
    }
}
