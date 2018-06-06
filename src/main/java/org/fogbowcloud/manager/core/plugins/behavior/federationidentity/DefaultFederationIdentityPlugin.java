package org.fogbowcloud.manager.core.plugins.behavior.federationidentity;

import org.fogbowcloud.manager.core.models.token.FederationUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DefaultFederationIdentityPlugin implements FederationIdentityPlugin {

    private Properties properties;
    
    public DefaultFederationIdentityPlugin(Properties properties) {
        this.properties = properties;
    }
    
    @Override
    public String createFederationTokenValue(Map<String, String> userCredentials) {
        return null;
    }

    @Override
    public FederationUser getFederationUser(String federationTokenValue) {
        Map<String, String> attributes = new HashMap();

        attributes.put("name", "default");

        return new FederationUser("default", attributes);
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        return true;
    }
}
