package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;

public class AaaPluginsHolder {
    private TokenGeneratorPlugin tokenGeneratorPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;
    private AuthenticationPlugin authenticationPlugin;
    private AuthorizationPlugin authorizationPlugin;

    public AaaPluginsHolder() {
    }

    public void setTokenGeneratorPlugin(TokenGeneratorPlugin tokenGeneratorPlugin) {
        this.tokenGeneratorPlugin = tokenGeneratorPlugin;
    }

    public void setFederationIdentityPlugin(FederationIdentityPlugin federationIdentityPlugin) {
        this.federationIdentityPlugin = federationIdentityPlugin;
    }

    public void setAuthenticationPlugin(AuthenticationPlugin authenticationPlugin) {
        this.authenticationPlugin = authenticationPlugin;
    }

    public void setAuthorizationPlugin(AuthorizationPlugin authorizationPlugin) {
        this.authorizationPlugin = authorizationPlugin;
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
}
