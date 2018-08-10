package org.fogbowcloud.manager.core.plugins.behavior.authentication;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin {
    
    public DefaultAuthenticationPlugin() {}

    @Override
    public boolean isAuthentic(String federationTokenValue) {
        return true;
    }
}
