package org.fogbowcloud.ras.core.plugins.aaa.identity.cloudstack;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;

public class CloudStackIdentityPlugin implements FederationIdentityPlugin<CloudStackToken> {
    @Override
    public CloudStackToken createToken(String tokenValue) throws InvalidParameterException {
        return null;
    }
}
