package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.api.http.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;

import java.util.Map;

public class StubGenericRequestRequestPlugin implements GenericRequestPlugin<Token> {

    public StubGenericRequestRequestPlugin() {
    }

    @Override
    public GenericRequestResponse redirectGenericRequest(String method, String url, Map<String, String> headers, Map<String, String> body, Token token) {
        return null;
    }

}
