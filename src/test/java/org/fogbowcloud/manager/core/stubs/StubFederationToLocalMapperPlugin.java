package org.fogbowcloud.manager.core.stubs;

import java.util.Map;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

/**
 * This class is a stub for the FederationToLocalMapperPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubFederationToLocalMapperPlugin implements FederationToLocalMapperPlugin {

    public StubFederationToLocalMapperPlugin() {}

    @Override
    public Token getToken(FederationUser user) { return null; }
}
