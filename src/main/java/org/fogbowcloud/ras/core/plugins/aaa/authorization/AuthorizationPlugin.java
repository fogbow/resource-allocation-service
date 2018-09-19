package org.fogbowcloud.ras.core.plugins.aaa.authorization;

import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public interface AuthorizationPlugin<T extends FederationUserToken> {
    /**
     * Verifies if the user described by federationTokenValue is authorized to perform the operation on the
     * type of resource indicated.
     *
     * @param federationUserToken the token describing the user to be authorized
     * @param operation the operation the user is requesting to perform
     * @param type the type of resources on which the operation will be executed
     * @return a boolean stating whether the user is authorized or not.
     */
    public boolean isAuthorized(T federationUserToken, Operation operation, ResourceType type);
}
