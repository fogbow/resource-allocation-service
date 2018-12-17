package org.fogbowcloud.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.HttpBasedGenericRequestPlugin;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

public class OpenStackGenericRequestPlugin extends HttpBasedGenericRequestPlugin<OpenStackV3Token> {

    public OpenStackGenericRequestPlugin() {
    }

    @Override
    public GenericRequestHttpResponse redirectGenericRequest(GenericRequest genericRequest, OpenStackV3Token token) throws FogbowRasException {
        Map<String, String> headers = genericRequest.getHeaders();
        headers.put(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());
        return getClient().doGenericRequest(genericRequest.getMethod(), genericRequest.getUrl(), headers, genericRequest.getBody());
    }

}
