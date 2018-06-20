package org.fogbowcloud.manager.core.plugins.behavior.federationidentity;

import java.util.HashMap;
import java.util.Map;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class DefaultFederationIdentityPlugin implements FederationIdentityPlugin {
    
    public DefaultFederationIdentityPlugin() {}
    
    @Override
    public String createFederationTokenValue(Map<String, String> userCredentials) {
        return "fake-token";
    }

    @Override
    public FederationUser getFederationUser(String federationTokenValue) {
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "default");

        return new FederationUser("default-id", attributes);
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        return true;
    }
}
