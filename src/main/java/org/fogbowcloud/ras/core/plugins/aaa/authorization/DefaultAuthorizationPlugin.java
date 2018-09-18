package org.fogbowcloud.ras.core.plugins.aaa.authorization;

import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public class DefaultAuthorizationPlugin implements AuthorizationPlugin<FederationUserToken> {

    public DefaultAuthorizationPlugin() {
    }

    @Override
    public boolean isAuthorized(FederationUserToken federationUserToken, Operation operation, ResourceType type) {
        return true;
    }
}
