package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

public class BehaviorPluginsHolder {

    private AuthorizationPlugin authorizationPlugin;
    private AuthenticationPlugin authenticationPlugin;
    private FederationToLocalMapperPlugin federationToLocalMapperPlugin;

    public BehaviorPluginsHolder(PluginInstantiator instantiationInitService) {
        this.authorizationPlugin = instantiationInitService.getAuthorizationPlugin();
        this.authenticationPlugin = instantiationInitService.getFederationIdentityPlugin();
        this.federationToLocalMapperPlugin = instantiationInitService.getLocalUserCredentialsMapperPlugin();
    }

    public AuthorizationPlugin getAuthorizationPlugin() {
        return this.authorizationPlugin;
    }

    public AuthenticationPlugin getAuthenticationPlugin() {
        return this.authenticationPlugin;
    }

    public FederationToLocalMapperPlugin getFederationToLocalMapperPlugin() {
        return this.federationToLocalMapperPlugin;
    }
}
