package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

/**
 * This class is a stub for the FederationToLocalMapperPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubFederationToLocalMapperPlugin implements FederationToLocalMapperPlugin {

    public StubFederationToLocalMapperPlugin() {}

    @Override
    public LocalUserAttributes map(FederationUserAttributes user) { return null; }
}
