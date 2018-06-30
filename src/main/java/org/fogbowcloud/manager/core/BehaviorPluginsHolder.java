package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;

public class BehaviorPluginsHolder {

    private AuthorizationPlugin authorizationPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;
    private LocalUserCredentialsMapperPlugin localUserCredentialsMapperPlugin;

    public BehaviorPluginsHolder(PluginInstantiator instantiationInitService) {
        this.authorizationPlugin = instantiationInitService.getAuthorizationPlugin();
        this.federationIdentityPlugin = instantiationInitService.getFederationIdentityPlugin();
        this.localUserCredentialsMapperPlugin = instantiationInitService.getLocalUserCredentialsMapperPlugin();
    }

    public AuthorizationPlugin getAuthorizationPlugin() {
        return this.authorizationPlugin;
    }

    public FederationIdentityPlugin getFederationIdentityPlugin() {
        return this.federationIdentityPlugin;
    }

    public LocalUserCredentialsMapperPlugin getLocalUserCredentialsMapperPlugin() {
        return this.localUserCredentialsMapperPlugin;
    }
}
