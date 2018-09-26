package org.fogbowcloud.ras.core.plugins.interoperability;

import java.util.Map;

public interface GenericPlugin {

    void redirectGenericRequest(String method, String url, Map<String, String> headers, String body);

}
