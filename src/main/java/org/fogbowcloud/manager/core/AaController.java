package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;

public class AaController {

    private static final Logger LOGGER = Logger.getLogger(AaController.class);

    private AuthenticationPlugin authenticationPlugin;
    private AuthorizationPlugin authorizationPlugin;

    public AaController(BehaviorPluginsHolder behaviorPluginsHolder) {
        this.authenticationPlugin = behaviorPluginsHolder.getAuthenticationPlugin();
        this.authorizationPlugin = behaviorPluginsHolder.getAuthorizationPlugin();
    }

    public FederationUser getFederationUser(String federationTokenValue)
            throws UnauthenticatedUserException, InvalidParameterException {
        return this.authenticationPlugin.getFederationUser(federationTokenValue);
    }

    public void authenticate(String federationTokenId) throws UnauthenticatedUserException {
        if (!this.authenticationPlugin.isValid(federationTokenId)) {
            throw new UnauthenticatedUserException();
        }
    }

    public void authorize(FederationUser federationUser, Operation operation, ResourceType type)
            throws UnauthorizedRequestException {
        if (!this.authorizationPlugin.isAuthorized(federationUser, operation, type)) {
            throw new UnauthorizedRequestException();
        }
    }
}
