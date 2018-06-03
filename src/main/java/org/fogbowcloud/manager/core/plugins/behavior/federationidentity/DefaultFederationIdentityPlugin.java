package org.fogbowcloud.manager.core.plugins.behavior.federationidentity;

import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenValueCreationException;
import org.fogbowcloud.manager.core.models.token.FederationUser;

import java.util.HashMap;
import java.util.Map;

public class DefaultFederationIdentityPlugin implements FederationIdentityPlugin {
    @Override
    public String createFederationTokenValue(Map<String, String> userCredentials) throws UnauthenticatedException, TokenValueCreationException {
        return null;
    }

    @Override
    public FederationUser getFederationUser(String federationTokenValue) throws UnauthenticatedException {
        Map<String, String> attributes = new HashMap();

        attributes.put("user_name", "default_user");

        return new FederationUser(0L, attributes);
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        return true;
    }
}
