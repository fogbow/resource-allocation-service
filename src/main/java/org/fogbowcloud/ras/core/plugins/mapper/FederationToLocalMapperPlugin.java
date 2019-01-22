package org.fogbowcloud.ras.core.plugins.mapper;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface FederationToLocalMapperPlugin<T extends Token> {
    public T map(FederationUserToken token) throws UnexpectedException, FogbowRasException;
}
