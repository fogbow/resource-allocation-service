package org.fogbowcloud.manager.core.stubs;

import java.util.Map;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;

/**
 * This class is a stub for the LocalUserCredentialsMapperPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubLocalUserCredentialsMapperPlugin implements LocalUserCredentialsMapperPlugin {

    public StubLocalUserCredentialsMapperPlugin() {}
    
    @Override
    public Map<String, String> getCredentials(FederationUser federationUser) {
        return null;
    }

}
