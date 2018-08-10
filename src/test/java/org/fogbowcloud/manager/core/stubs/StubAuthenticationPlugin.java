package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;

/**
 * This class is a stub for the AuthenticationPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAuthenticationPlugin implements AuthenticationPlugin {

    public StubAuthenticationPlugin() {}

//    @Override
//    public FederationUserToken getFederationUser(String federationTokenValue)
//            throws UnauthenticatedUserException, InvalidParameterException {
//        return null;
//    }

    @Override
    public boolean isAuthentic(String federationTokenValue) {
        return false;
    }

}
