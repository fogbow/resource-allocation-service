package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;

public class AaaPluginsHolder {
    private TokenGeneratorPlugin tokenGeneratorPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;
    private AuthenticationPlugin authenticationPlugin;
    private AuthorizationPlugin authorizationPlugin;
    private FederationToLocalMapperPlugin federationToLocalMapperPlugin;

    public AaaPluginsHolder(PluginInstantiator instantiationInitService) {
        this.tokenGeneratorPlugin = instantiationInitService.getTokenGeneratorPlugin();
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
