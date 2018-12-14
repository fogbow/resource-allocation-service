package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;

import java.util.Map;

public interface GenericRequestPlugin<Token> {

    GenericRequestResponse redirectGenericRequest(String method, String url, Map<String, String> headers, Map<String, String> body, Token token) throws FogbowRasException;

}
