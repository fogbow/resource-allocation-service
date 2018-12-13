package org.fogbowcloud.ras.core.plugins.interoperability.openstack.genericplugin.v2;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
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
    public String redirectGenericRequest(String method, String url, Map<String, String> headers, Map<String, String> body, OpenStackV3Token token) throws FogbowRasException {
        headers.put(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());
        return getClient().doGenericRequest(method, url, headers, body);
    }

}
