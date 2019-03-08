package cloud.fogbow.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.common.util.connectivity.HttpRequest;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import com.google.gson.Gson;

public class OpenStackGenericRequestPlugin implements GenericRequestPlugin<OpenStackV3User> {

    private OpenStackHttpClient client;

    public OpenStackGenericRequestPlugin() {
        this.client = new OpenStackHttpClient();
    }

    @Override
    public HttpResponse redirectGenericRequest(String genericRequest, OpenStackV3User cloudUser) throws FogbowException {
        HttpRequest httpRequest = new Gson().fromJson(genericRequest, HttpRequest.class);
        if (httpRequest.getHeaders().containsKey(OpenStackConstants.X_AUTH_TOKEN_KEY)) {
            throw new InvalidParameterException(Messages.Exception.TOKEN_ALREADY_SPECIFIED);
        }

        return client.doGenericRequest(httpRequest.getMethod(), httpRequest.getUrl(),
                httpRequest.getHeaders(), httpRequest.getBody(), cloudUser);
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

}
