package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import java.util.Map;

public abstract class HttpBasedGenericRequestPlugin implements GenericRequestPlugin {

    @Override
    public abstract void redirectGenericRequest(String method, String url, Map<String, String> headers, String body);

}
