package org.fogbowcloud.ras.core.plugins.aaa.identity;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public class DefaultFederationIdentityPlugin implements FederationIdentityPlugin {

    @Override
    public FederationUserToken createToken(String tokenValue) {
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        return new FederationUserToken(provider, tokenValue, "fake-user-id", "fake-name");
    }
}
