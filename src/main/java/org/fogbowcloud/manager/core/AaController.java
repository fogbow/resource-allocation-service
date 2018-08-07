package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;
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

    public FederationUserAttributes getFederationUser(String federationTokenValue)
            throws UnauthenticatedUserException, InvalidParameterException {
        return this.authenticationPlugin.getFederationUser(federationTokenValue);
    }

    public void authenticate(String federationTokenId) throws UnauthenticatedUserException {
        if (!this.authenticationPlugin.isValid(federationTokenId)) {
            throw new UnauthenticatedUserException();
        }
    }

    public void authorize(FederationUserAttributes federationUserAttributes, Operation operation, ResourceType type)
            throws UnauthorizedRequestException {
        if (!this.authorizationPlugin.isAuthorized(federationUserAttributes, operation, type)) {
            throw new UnauthorizedRequestException();
        }
    }
}
