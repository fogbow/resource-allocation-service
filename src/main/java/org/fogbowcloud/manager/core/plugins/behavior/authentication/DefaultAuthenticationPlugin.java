package org.fogbowcloud.manager.core.plugins.behavior.authentication;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin {
    
    public DefaultAuthenticationPlugin() {}

    @Override
    public FederationUser getFederationUser(String federationTokenValue) throws InvalidParameterException {
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "default");

        return new FederationUser("default-id", attributes);
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        return true;
    }
}
