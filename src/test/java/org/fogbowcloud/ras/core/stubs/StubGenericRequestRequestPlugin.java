package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.api.http.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;

import java.util.Map;

public class StubGenericRequestRequestPlugin implements GenericRequestPlugin<Token> {

    public StubGenericRequestRequestPlugin(String confFilePath) {
    }

    @Override
    public String redirectGenericRequest(String method, String url, Map<String, String> headers, String body, Token token) {
        return null;
    }

}
