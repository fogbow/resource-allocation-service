package cloud.fogbow.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpFogbowGenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpClient;

public class OpenStackGenericRequestPlugin implements GenericRequestPlugin<HttpFogbowGenericRequest, OpenStackV3User> {

    private OpenStackHttpClient client;

    public OpenStackGenericRequestPlugin() {
        this.client = new OpenStackHttpClient();
    }

    @Override
    public HttpResponse redirectGenericRequest(HttpFogbowGenericRequest genericRequest, OpenStackV3User cloudUser)
            throws FogbowException {
        if (genericRequest.getHeaders().containsKey(OpenStackConstants.X_AUTH_TOKEN_KEY)) {
            throw new InvalidParameterException(Messages.Exception.TOKEN_ALREADY_SPECIFIED);
        }

        return client.doGenericRequest(genericRequest.getMethod(), genericRequest.getUrl(),
                genericRequest.getHeaders(), genericRequest.getBody(), cloudUser);
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

}
