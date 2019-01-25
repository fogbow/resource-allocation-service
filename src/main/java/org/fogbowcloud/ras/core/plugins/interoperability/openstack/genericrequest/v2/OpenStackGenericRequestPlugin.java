package org.fogbowcloud.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.HttpBasedGenericRequestPlugin;
import org.fogbowcloud.ras.util.connectivity.AuditableHttpRequestClient;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;

import java.util.Map;

public class OpenStackGenericRequestPlugin extends HttpBasedGenericRequestPlugin<OpenStackV3Token> {
    @Override
    public GenericRequestHttpResponse redirectGenericRequest(GenericRequest genericRequest, OpenStackV3Token token) throws FogbowRasException {
        Map<String, String> headers = genericRequest.getHeaders();

        if (headers.containsKey(HttpRequestUtil.X_AUTH_TOKEN_KEY)) {
            throw new InvalidParameterException(Messages.Exception.TOKEN_ALREADY_SPECIFIED);
        }

        headers.put(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());
        return getClient().doGenericRequest(genericRequest.getMethod(), genericRequest.getUrl(), headers, genericRequest.getBody());
    }

    @Override
    protected void setClient(AuditableHttpRequestClient auditableHttpRequestClient) {
        super.setClient(auditableHttpRequestClient);
    }
}
