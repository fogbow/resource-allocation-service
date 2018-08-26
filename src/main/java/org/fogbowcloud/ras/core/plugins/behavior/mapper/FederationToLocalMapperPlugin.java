package org.fogbowcloud.ras.core.plugins.behavior.mapper;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface FederationToLocalMapperPlugin {

    public Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException;
}
