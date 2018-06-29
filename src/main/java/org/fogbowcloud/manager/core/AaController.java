package org.fogbowcloud.manager.core;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;

public class AaController {

    private static final Logger LOGGER = Logger.getLogger(AaController.class);

    private FederationIdentityPlugin federationIdentityPlugin;
    private LocalIdentityPlugin localIdentityPlugin;
    private AuthorizationPlugin authorizationPlugin;
    private LocalUserCredentialsMapperPlugin localUserCredentialsMapperPlugin;

    public AaController(LocalIdentityPlugin localIdentityPlugin, BehaviorPluginsHolder behaviorPluginsHolder) {
        this.localIdentityPlugin = localIdentityPlugin;
        this.federationIdentityPlugin = behaviorPluginsHolder.getFederationIdentityPlugin();
        this.localUserCredentialsMapperPlugin = behaviorPluginsHolder.getLocalUserCredentialsMapperPlugin();
        this.authorizationPlugin = behaviorPluginsHolder.getAuthorizationPlugin();
    }

    public FederationUser getFederationUser(String federationTokenValue) throws UnauthenticatedUserException {
        LOGGER.debug(
                "Trying to get the federationidentity token by federationidentity token id: " + federationTokenValue);
        return this.federationIdentityPlugin.getFederationUser(federationTokenValue);
    }

    public Token getLocalToken(FederationUser federationUser) throws FogbowManagerException {
    	Map<String, String> userCredentials = this.localUserCredentialsMapperPlugin.getCredentials(federationUser);
        return this.localIdentityPlugin.createToken(userCredentials);
    }

    public void authenticate(String federationTokenId) throws UnauthenticatedUserException {
        if (!this.federationIdentityPlugin.isValid(federationTokenId)) {
            throw new UnauthenticatedUserException();
        }
    }

    public void authorize(FederationUser federationUser, Operation operation, InstanceType type)
            throws UnauthorizedRequestException {
        if (!this.authorizationPlugin.isAuthorized(federationUser, operation, type)) {
            throw new UnauthorizedRequestException();
        }
    }
    
    public void authorize(FederationUser federationUser, Operation operation)
            throws UnauthorizedRequestException {
        if (!this.authorizationPlugin.isAuthorized(federationUser, operation)) {
            throw new UnauthorizedRequestException();
        }
    }    

    public void authorize(FederationUser federationUser, Operation operation, Order order)
            throws FogbowManagerException {
        if (!this.authorizationPlugin.isAuthorized(federationUser, operation, order)) {
            throw new UnauthorizedRequestException();
        }
    }
}
