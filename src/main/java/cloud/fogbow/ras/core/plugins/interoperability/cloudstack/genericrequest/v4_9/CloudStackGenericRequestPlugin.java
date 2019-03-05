package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.common.util.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.common.util.connectivity.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;

public class CloudStackGenericRequestPlugin implements GenericRequestPlugin<CloudUser> {

    private CloudStackHttpClient client;

    public CloudStackGenericRequestPlugin() {
        this.client = new CloudStackHttpClient();
    }

    @Override
    public HttpResponse redirectGenericRequest(GenericRequest genericRequest, CloudUser token) throws FogbowException {
        try {
            URIBuilder uriBuilder = new URIBuilder(genericRequest.getUrl());
            CloudStackUrlUtil.sign(uriBuilder, token.getToken());

            String url = uriBuilder.toString();
            HashMap<String, String> headers = genericRequest.getHeaders();
            HashMap<String, String> body = genericRequest.getBody();
            return client.doGenericRequest(HttpMethod.GET, url, headers, body, token);
        } catch (URISyntaxException e) {
            throw new FogbowException(String.format(Messages.Exception.MALFORMED_GENERIC_REQUEST_URL, genericRequest.getUrl()));
        }
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}