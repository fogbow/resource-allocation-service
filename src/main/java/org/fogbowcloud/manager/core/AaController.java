package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;

public class AaController {

    private static final Logger LOGGER = Logger.getLogger(AaController.class);

    private AuthenticationPlugin authenticationPlugin;
    private AuthorizationPlugin authorizationPlugin;

    public AaController(BehaviorPluginsHolder behaviorPluginsHolder) {
        this.authenticationPlugin = behaviorPluginsHolder.getAuthenticationPlugin();
        this.authorizationPlugin = behaviorPluginsHolder.getAuthorizationPlugin();
    }

    public FederationUserToken getFederationUser(String federationTokenValue)
            throws UnauthenticatedUserException, InvalidParameterException {
        return this.authenticationPlugin.getFederationUser(federationTokenValue);
    }

    public void authenticate(String federationTokenId) throws UnauthenticatedUserException {
        if (!this.authenticationPlugin.isAuthentic(federationTokenId)) {
            throw new UnauthenticatedUserException();
        }
    }

    public void authorize(FederationUserToken federationUserToken, Operation operation, ResourceType type)
            throws UnauthorizedRequestException {
        if (!this.authorizationPlugin.isAuthorized(federationUserToken, operation, type)) {
            throw new UnauthorizedRequestException();
        }
    }
}
