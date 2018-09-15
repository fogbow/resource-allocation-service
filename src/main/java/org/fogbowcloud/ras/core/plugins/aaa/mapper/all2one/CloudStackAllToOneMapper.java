package org.fogbowcloud.ras.core.plugins.aaa.mapper.all2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.identity.cloudstack.CloudStackIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;

public class CloudStackAllToOneMapper implements FederationToLocalMapperPlugin {
    private GenericAllToOneFederationToLocalMapper genericMapper;

    public CloudStackAllToOneMapper() {
        this.genericMapper = new GenericAllToOneFederationToLocalMapper(new CloudStackTokenGeneratorPlugin(),
                new CloudStackIdentityPlugin(), "cloudstack-mapper.conf");
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return genericMapper.map(token);
    }

    //Used in testing
    protected void setGenericMapper(GenericAllToOneFederationToLocalMapper mapper) {
        this.genericMapper = mapper;
    }
}
