package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpBasedGenericRequestPlugin;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHttpRequest;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.springframework.http.HttpStatus;

import java.net.URISyntaxException;

public class CloudStackGenericRequestPlugin extends HttpBasedGenericRequestPlugin {

    @Override
    public GenericRequestHttpResponse redirectGenericRequest(GenericRequest genericRequest, CloudToken token) throws FogbowException {
        String url = genericRequest.getUrl();
        BasicHttpRequest request = new BasicHttpRequest(genericRequest.getMethod(), url);
        for (String headerKey : genericRequest.getHeaders().keySet()) {
            request.setHeader(headerKey, genericRequest.getHeaders().get(headerKey));
        }

        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            CloudStackUrlUtil.sign(uriBuilder, token.getTokenValue());
            try {
                return new GenericRequestHttpResponse(getClient().doGetRequest(uriBuilder.toString(), token), HttpStatus.OK.value());
            } catch (HttpResponseException e) {
                return new GenericRequestHttpResponse(e.getMessage(), e.getStatusCode());
            }
        } catch (URISyntaxException e) {
            throw new FogbowException(String.format(Messages.Exception.MALFORMED_GENERIC_REQUEST_URL, url));
        }
    }

    @Override
    protected void setClient(AuditableHttpRequestClient auditableHttpRequestClient) {
        super.setClient(auditableHttpRequestClient);
    }
}