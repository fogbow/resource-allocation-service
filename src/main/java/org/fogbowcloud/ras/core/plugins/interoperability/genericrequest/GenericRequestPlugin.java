package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import java.util.Map;

public interface GenericRequestPlugin {

    void redirectGenericRequest(String method, String url, Map<String, String> headers, String body);

}
