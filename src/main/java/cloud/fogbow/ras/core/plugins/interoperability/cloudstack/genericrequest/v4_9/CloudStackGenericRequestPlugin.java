package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.GenericRequestHttpResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;

public class CloudStackGenericRequestPlugin implements GenericRequestPlugin<CloudToken> {

    public static final String CLOUDSTACK_HTTP_METHOD = "GET";

    private CloudStackHttpClient client;

    @Override
    public GenericRequestHttpResponse redirectGenericRequest(GenericRequest genericRequest, CloudToken token) throws FogbowException {
        try {
            URIBuilder uriBuilder = new URIBuilder(genericRequest.getUrl());
            CloudStackUrlUtil.sign(uriBuilder, token.getTokenValue());

            String url = uriBuilder.toString();
            HashMap<String, String> headers = genericRequest.getHeaders();
            HashMap<String, String> body = genericRequest.getBody();
            return client.doGenericRequest(CLOUDSTACK_HTTP_METHOD, url, headers, body, token);
        } catch (URISyntaxException e) {
            throw new FogbowException(String.format(Messages.Exception.MALFORMED_GENERIC_REQUEST_URL, genericRequest.getUrl()));
        }
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}