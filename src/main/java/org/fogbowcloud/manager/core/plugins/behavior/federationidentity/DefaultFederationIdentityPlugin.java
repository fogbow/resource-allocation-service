package org.fogbowcloud.manager.core.plugins.behavior.federationidentity;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

public class DefaultFederationIdentityPlugin implements FederationIdentityPlugin {
    
    public DefaultFederationIdentityPlugin() {}
    
    @Override
    public String createFederationTokenValue(Map<String, String> userCredentials) {
        return "fake-tokens";
    }

    @Override
    public FederationUser getFederationUser(String federationTokenValue) throws UnexpectedException {
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "default");

        return new FederationUser("default-id", attributes);
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        return true;
    }
}
