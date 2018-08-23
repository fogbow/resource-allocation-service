package org.fogbowcloud.manager.core.plugins.behavior.identity;

import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public class DefaultFederationIdentityPlugin implements FederationIdentityPlugin {

    @Override
    public FederationUserToken createToken(String tokenValue) {
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        return new FederationUserToken(provider, tokenValue, "fake-user-id", "fake-name");
    }
}
