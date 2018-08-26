package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.models.tokens.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

public class BehaviorPluginsHolder {
    private TokenGeneratorPlugin tokenGeneratorPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;
    private AuthenticationPlugin authenticationPlugin;
    private AuthorizationPlugin authorizationPlugin;
    private FederationToLocalMapperPlugin federationToLocalMapperPlugin;

    public BehaviorPluginsHolder(PluginInstantiator instantiationInitService) {
        this.tokenGeneratorPlugin = instantiationInitService.getTokenGenerator();
        this.federationIdentityPlugin = instantiationInitService.getFederationIdentityPlugin();
        this.authenticationPlugin = instantiationInitService.getAuthenticationPlugin();
        this.authorizationPlugin = instantiationInitService.getAuthorizationPlugin();
        this.federationToLocalMapperPlugin = instantiationInitService.getLocalUserCredentialsMapperPlugin();
    }

    public TokenGeneratorPlugin getTokenGeneratorPlugin() {
        return this.tokenGeneratorPlugin;
    }

    public FederationIdentityPlugin getFederationIdentityPlugin() {
        return this.federationIdentityPlugin;
    }

    public AuthenticationPlugin getAuthenticationPlugin() {
        return this.authenticationPlugin;
    }

    public AuthorizationPlugin getAuthorizationPlugin() {
        return this.authorizationPlugin;
    }

    public FederationToLocalMapperPlugin getFederationToLocalMapperPlugin() {
        return this.federationToLocalMapperPlugin;
    }
}
