package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpBasedGenericRequestPlugin;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.Map;

public class CloudStackGenericRequestPlugin extends HttpBasedGenericRequestPlugin {

    public static final String CLOUDSTACK_HTTP_METHOD = "GET";

    @Override
    public GenericRequestHttpResponse redirectGenericRequest(GenericRequest genericRequest, CloudToken token) throws FogbowException {
        try {
            URIBuilder uriBuilder = new URIBuilder(genericRequest.getUrl());
            CloudStackUrlUtil.sign(uriBuilder, token.getTokenValue());

            String url = uriBuilder.toString();
            Map<String, String> headers = genericRequest.getHeaders();
            Map<String, String> body = genericRequest.getBody();
            return getClient().doGenericRequest(CLOUDSTACK_HTTP_METHOD, url, headers, body, token);
        } catch (URISyntaxException e) {
            throw new FogbowException(String.format(Messages.Exception.MALFORMED_GENERIC_REQUEST_URL, genericRequest.getUrl()));
        }
    }

    @Override
    protected void setClient(AuditableHttpRequestClient auditableHttpRequestClient) {
        super.setClient(auditableHttpRequestClient);
    }
}