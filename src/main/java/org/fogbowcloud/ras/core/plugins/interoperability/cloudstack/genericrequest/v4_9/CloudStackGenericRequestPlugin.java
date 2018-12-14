package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HTTP;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.HttpBasedGenericRequestPlugin;
import org.springframework.http.HttpStatus;

import java.net.URISyntaxException;
import java.util.Map;

public class CloudStackGenericRequestPlugin extends HttpBasedGenericRequestPlugin<CloudStackToken> {

    @Override
    public GenericRequestHttpResponse redirectGenericRequest(String method, String url, Map<String, String> headers, Map<String, String> body, CloudStackToken token) throws FogbowRasException {
        BasicHttpRequest request = new BasicHttpRequest(method, url);
        for (String headerKey : headers.keySet()) {
            request.setHeader(headerKey, headers.get(headerKey));
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
            throw new FogbowRasException(String.format(Messages.Exception.MALFORMED_GENERIC_REQUEST_URL, url));
        }
    }

}