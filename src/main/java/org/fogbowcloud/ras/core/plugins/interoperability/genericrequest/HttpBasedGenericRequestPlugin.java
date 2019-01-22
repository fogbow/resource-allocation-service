package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.Map;

public abstract class HttpBasedGenericRequestPlugin<T> implements GenericRequestPlugin<T> {

    private HttpRequestClientUtil client = new HttpRequestClientUtil();

    @Override
    public abstract GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, T token) throws FogbowRasException;

    protected HttpRequestClientUtil getClient() {
        return client;
    }

    protected void setClient(HttpRequestClientUtil httpRequestClientUtil) {
        this.client = httpRequestClientUtil;
    }
}
