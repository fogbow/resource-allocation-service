package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;

public abstract class HttpBasedGenericRequestPlugin implements GenericRequestPlugin {

    private AuditableHttpRequestClient client = new AuditableHttpRequestClient(
            new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                                                                       ConfigurationPropertyDefaults.XMPP_TIMEOUT)));

    @Override
    public abstract GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, CloudToken token)
            throws FogbowException;

    protected AuditableHttpRequestClient getClient() {
        return client;
    }

    protected void setClient(AuditableHttpRequestClient auditableHttpRequestClient) {
        this.client = auditableHttpRequestClient;
    }
}
