package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

public class BehaviorPluginsHolder {

    private AuthorizationPlugin authorizationPlugin;
    private AuthenticationPlugin authenticationPlugin;
    private FederationToLocalMapperPlugin federationToLocalMapperPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;

    public BehaviorPluginsHolder(PluginInstantiator instantiationInitService) {
        this.authorizationPlugin = instantiationInitService.getAuthorizationPlugin();
        this.authenticationPlugin = instantiationInitService.getAuthenticationPlugin();
        this.federationToLocalMapperPlugin = instantiationInitService.getLocalUserCredentialsMapperPlugin();
        this.federationIdentityPlugin = instantiationInitService.getFederationIdentityPlugin();
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

    public FederationIdentityPlugin getFederationIdentityPlugin() {
        return this.federationIdentityPlugin;
    }
}
