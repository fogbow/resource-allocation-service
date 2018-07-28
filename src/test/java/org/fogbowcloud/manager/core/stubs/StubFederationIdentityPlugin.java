package org.fogbowcloud.manager.core.stubs;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.TokenValueCreationException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;

/**
 * This class is a stub for the FederationIdentityPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubFederationIdentityPlugin implements FederationIdentityPlugin {

    public StubFederationIdentityPlugin() {}
    
    @Override
    public String createFederationTokenValue(Map<String, String> userCredentials)
            throws UnauthenticatedUserException, TokenValueCreationException {
        return null;
    }

    @Override
    public FederationUser getFederationUser(String federationTokenValue)
            throws UnauthenticatedUserException, InvalidParameterException {
        return null;
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        return false;
    }

}
