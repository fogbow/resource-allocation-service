package org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack.v3;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;

public class OpenStackAuthenticationPlugin extends RASAuthenticationPlugin {

	private static final Logger LOGGER = Logger.getLogger(OpenStackAuthenticationPlugin.class);

    public OpenStackAuthenticationPlugin() {
    }

    @Override
    protected String getTokenMessage(FederationUserToken federationUserToken) {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) federationUserToken;
        String[] parameters = new String[] {
                openStackV3Token.getTokenProvider(),
                openStackV3Token.getTokenValue(),
                openStackV3Token.getUserId(),
                openStackV3Token.getUserName(),
                openStackV3Token.getProjectId()};
        return StringUtils.join(parameters, OpenStackTokenGeneratorPlugin.OPENSTACK_TOKEN_STRING_SEPARATOR);
    }

    @Override
    protected String getSignature(FederationUserToken federationUserToken) {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) federationUserToken;
        return openStackV3Token.getSignature();
    }
}
