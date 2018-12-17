package org.fogbowcloud.ras.core.plugins.interoperability.genericrequest;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;

import java.util.Map;

public interface GenericRequestPlugin<Token> {

    GenericRequestResponse redirectGenericRequest(GenericRequest genericRequest, Token token) throws FogbowRasException;

}
