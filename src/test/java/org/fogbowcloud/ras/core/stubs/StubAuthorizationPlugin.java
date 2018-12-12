package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;

/**
 * This class is allocationAllowableValues stub for the AuthorizationPlugin interface used for tests only.
 * Should not have allocationAllowableValues proper implementation.
 */
public class StubAuthorizationPlugin implements AuthorizationPlugin {

    public StubAuthorizationPlugin() {
    }

    @Override
    public boolean isAuthorized(FederationUserToken federationUserToken, String cloudName, Operation operation, ResourceType type) {
        return true;
    }

    @Override
    public boolean isAuthorized(FederationUserToken federationUserToken, Operation operation, ResourceType type) {
        return true;
    }
}
