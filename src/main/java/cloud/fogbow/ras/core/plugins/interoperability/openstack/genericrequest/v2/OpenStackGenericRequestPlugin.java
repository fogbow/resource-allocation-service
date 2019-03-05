package cloud.fogbow.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.common.util.connectivity.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import cloud.fogbow.common.util.cloud.openstack.OpenStackHttpClient;

public class OpenStackGenericRequestPlugin implements GenericRequestPlugin<CloudUser> {

    private OpenStackHttpClient client;

    public OpenStackGenericRequestPlugin() {
        this.client = new OpenStackHttpClient();
    }

    @Override
    public HttpResponse redirectGenericRequest(GenericRequest genericRequest, CloudUser token)
            throws FogbowException {
        if (genericRequest.getHeaders().containsKey(OpenStackConstants.X_AUTH_TOKEN_KEY)) {
            throw new InvalidParameterException(Messages.Exception.TOKEN_ALREADY_SPECIFIED);
        }

        return client.doGenericRequest(genericRequest.getMethod(), genericRequest.getUrl(),
                genericRequest.getHeaders(), genericRequest.getBody(), token);
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
}
