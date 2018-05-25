package org.fogbowcloud.manager.core.services;

import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.manager.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.manager.plugins.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;

// TODO change the name.
public class AAAController {

    private static final Logger LOGGER = Logger.getLogger(AAAController.class);

    private FederationIdentityPlugin federationIdentityPlugin;
    private LocalIdentityPlugin localIdentityPlugin;
    private AuthorizationPlugin authorizationPlugin;
    private Properties properties;

    public AAAController(
            FederationIdentityPlugin federationIdentityPlugin,
            LocalIdentityPlugin localIdentityPlugin,
            AuthorizationPlugin authorizationPlugin,
            Properties properties) {
        // TODO check if there are the local token properties
        this.federationIdentityPlugin = federationIdentityPlugin;
        this.localIdentityPlugin = localIdentityPlugin;
        this.authorizationPlugin = authorizationPlugin;
        this.properties = properties;
    }

    public FederationUser getFederationUser(String federationTokenValue)
            throws UnauthenticatedException, UnauthorizedException {
        LOGGER.debug(
                "Trying to get the federation token by federation token id: " + federationTokenValue);
        return this.federationIdentityPlugin.getFederationUser(federationTokenValue);
    }

    private Map<String, String> getDefaultUserCredentials() throws PropertyNotSpecifiedException {
        return AuthenticationControllerUtil.getDefaultLocalTokenCredentials(this.properties);
    }

    public Token getLocalToken()
            throws PropertyNotSpecifiedException, UnauthorizedException, TokenCreationException {
        Map<String, String> userCredentials = getDefaultUserCredentials();
        return this.localIdentityPlugin.createToken(userCredentials);
    }

    public void authenticate(String federationTokenId) throws UnauthenticatedException {
        if (!this.federationIdentityPlugin.isValid(federationTokenId)) {
            throw new UnauthenticatedException();
        }
    }

    public void authorize(FederationUser federationUser, Operation operation, OrderType type)
            throws UnauthorizedException {
        if (!this.authorizationPlugin.isAuthorized(federationUser, operation, type)) {
            throw new UnauthorizedException();
        }
    }

    public void authorize(FederationUser federationUser, Operation operation, Order order)
            throws UnauthorizedException {
        if (!this.authorizationPlugin.isAuthorized(federationUser, operation, order)) {
            throw new UnauthorizedException();
        }
    }
}
