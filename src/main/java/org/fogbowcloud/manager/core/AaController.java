package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;

public class AaController {

    private static final Logger LOGGER = Logger.getLogger(AaController.class);

    private AuthenticationPlugin authenticationPlugin;
    private AuthorizationPlugin authorizationPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;

    public AaController(BehaviorPluginsHolder behaviorPluginsHolder) {
        this.authenticationPlugin = behaviorPluginsHolder.getAuthenticationPlugin();
        this.authorizationPlugin = behaviorPluginsHolder.getAuthorizationPlugin();
        this.federationIdentityPlugin = behaviorPluginsHolder.getFederationIdentityPlugin();
    }

    public void authenticateAndAuthorize(FederationUserToken requester,
                                         Operation operation, ResourceType type)
            throws UnauthenticatedUserException, UnauthorizedRequestException, InvalidParameterException {
        // Authenticate user based on the token received
        LOGGER.debug("calling authenticate");
        authenticate(requester.getTokenValue());
        // Authorize the user based on user's attributes, requested operation and resource type
        LOGGER.debug("calling authorize");
        authorize(requester, operation, type);
    }

    public void authenticateAndAuthorize(FederationUserToken requester, Operation operation, ResourceType type,
                                         Order order) throws FogbowManagerException {
        LOGGER.debug("executing authenticateAndAuthorize");
        // Check if requested type matches order type
        if (!order.getType().equals(type)) throw new InstanceNotFoundException("Mismatching resource type");
        // Authenticate user and get authorization to perform generic operation on the type of resource
        LOGGER.debug("calling authenticateAndAuthorize");
        authenticateAndAuthorize(requester, operation, type);
        // Check whether requester owns order
        FederationUserToken orderOwner = order.getFederationUserToken();
        if (!orderOwner.getUserId().equals(requester.getUserId())) {
            throw new UnauthorizedRequestException("Requester does not own order");
        }
    }

    public FederationUserToken getFederationUser(String federationTokenValue) throws UnauthenticatedUserException {
        return this.federationIdentityPlugin.createToken(federationTokenValue);
    }

    public void authenticate(String federationTokenId) throws UnauthenticatedUserException {
        if (!this.authenticationPlugin.isAuthentic(federationTokenId)) {
            throw new UnauthenticatedUserException();
        }
    }

    public void authorize(FederationUserToken federationUserToken, Operation operation, ResourceType type)
            throws UnauthorizedRequestException {
        LOGGER.debug("executing authorize");
        if (!this.authorizationPlugin.isAuthorized(federationUserToken, operation, type)) {
            throw new UnauthorizedRequestException();
        }
        LOGGER.debug("returned from authorized");
    }
}
