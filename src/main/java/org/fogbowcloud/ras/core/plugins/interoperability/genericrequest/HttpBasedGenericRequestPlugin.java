package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.util.connectivity.AuditableHttpRequestClient;

public abstract class HttpBasedGenericRequestPlugin<T> implements GenericRequestPlugin<T> {

    private AuditableHttpRequestClient client = new AuditableHttpRequestClient();

    @Override
    public abstract GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, T token) throws FogbowRasException;

    protected AuditableHttpRequestClient getClient() {
        return client;
    }

    protected void setClient(AuditableHttpRequestClient auditableHttpRequestClient) {
        this.client = auditableHttpRequestClient;
    }
}
