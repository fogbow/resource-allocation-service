package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;

import java.util.Map;

public interface GenericRequestPlugin<Token> {

    String redirectGenericRequest(String method, String url, Map<String, String> headers, String body, Token token) throws FogbowRasException;

}
