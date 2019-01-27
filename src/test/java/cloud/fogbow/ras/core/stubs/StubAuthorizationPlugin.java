package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;

/**
 * This class is a stub for the AuthorizationPlugin interface used for tests only.
 * Should not have a proper implementation.
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
