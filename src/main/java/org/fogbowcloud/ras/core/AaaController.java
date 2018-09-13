package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;

import java.util.Map;

public class AaaController {
    private AuthenticationPlugin authenticationPlugin;
    private AuthorizationPlugin authorizationPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;
    private TokenGeneratorPlugin tokenGeneratorPlugin;
    private String localMemberIdentity;

    public AaaController(AaaPluginsHolder aaaPluginsHolder) {
        this.tokenGeneratorPlugin = aaaPluginsHolder.getTokenGeneratorPlugin();
        this.authenticationPlugin = aaaPluginsHolder.getAuthenticationPlugin();
        this.authorizationPlugin = aaaPluginsHolder.getAuthorizationPlugin();
        this.federationIdentityPlugin = aaaPluginsHolder.getFederationIdentityPlugin();
        this.localMemberIdentity = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException,
            FogbowRasException {
        return this.tokenGeneratorPlugin.createTokenValue(userCredentials);
    }

    public void authenticateAndAuthorize(FederationUserToken requester,
                                         Operation operation, ResourceType type)
            throws UnauthenticatedUserException, UnauthorizedRequestException, UnavailableProviderException {
        // Authenticate user based on the token received
        authenticate(requester);
        // Authorize the user based on user's attributes, requested operation and resource type
        authorize(requester, operation, type);
    }

    public void authenticateAndAuthorize(FederationUserToken requester, Operation operation, ResourceType type,
                                         Order order) throws FogbowRasException {
        // Check if requested type matches order type
        if (!order.getType().equals(type)) throw new InstanceNotFoundException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
        // Check whether requester owns order
        FederationUserToken orderOwner = order.getFederationUserToken();
        if (!orderOwner.getUserId().equals(requester.getUserId())) {
            throw new UnauthorizedRequestException(Messages.Exception.REQUESTER_NOT_OWN_ORDER);
        }
        // Authenticate user and get authorization to perform generic operation on the type of resource
        authenticateAndAuthorize(requester, operation, type);
    }

    public void remoteAuthenticateAndAuthorize(FederationUserToken federationUserToken, Operation operation,
                                               ResourceType type, String memberId) throws FogbowRasException {
        if (!memberId.equals(this.localMemberIdentity)) {
            throw new InstanceNotFoundException(Messages.Exception.PROVIDING_MEMBER_NOT_CORRECT);
        } else {
            authenticateAndAuthorize(federationUserToken, operation, type);
        }
    }

    public void remoteAuthenticateAndAuthorize(FederationUserToken federationUserToken, Operation operation,
                                               ResourceType type, Order order) throws FogbowRasException {
        if (!order.getProvidingMember().equals(this.localMemberIdentity)) {
            throw new InstanceNotFoundException(Messages.Exception.PROVIDING_MEMBER_NOT_CORRECT);
        } else {
            authenticateAndAuthorize(federationUserToken, operation, type, order);
        }
    }

    public FederationUserToken getFederationUser(String federationTokenValue) throws InvalidParameterException {
        return this.federationIdentityPlugin.createToken(federationTokenValue);
    }

    public void authenticate(FederationUserToken federationToken) throws UnauthenticatedUserException,
            UnavailableProviderException {
        if (!this.authenticationPlugin.isAuthentic(federationToken)) {
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
