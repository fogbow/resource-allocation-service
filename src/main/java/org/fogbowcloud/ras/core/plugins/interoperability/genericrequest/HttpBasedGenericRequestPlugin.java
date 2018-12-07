package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.Map;

public abstract class HttpBasedGenericRequestPlugin<T> implements GenericRequestPlugin<T> {

    private final HttpRequestClientUtil client = new HttpRequestClientUtil();

    @Override
    public abstract String redirectGenericRequest(String method, String url, Map<String, String> headers, String body, T token) throws FogbowRasException;

    protected HttpRequestClientUtil getClient() {
        return client;
    }

}
