package org.fogbowcloud.ras.core.plugins.mapper.all2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.cloudstack.CloudStackIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;

public class CloudStackAllToOneMapper implements FederationToLocalMapperPlugin<CloudStackToken> {
    private GenericAllToOneFederationToLocalMapper genericMapper;

    public CloudStackAllToOneMapper(String interoperabilityConfFilePath, String mapperConfFilePath) {
        this.genericMapper = new GenericAllToOneFederationToLocalMapper(
                new CloudStackTokenGeneratorPlugin(interoperabilityConfFilePath),
                new CloudStackIdentityPlugin(), mapperConfFilePath);
    }

    @Override
    public CloudStackToken map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return (CloudStackToken) genericMapper.map(token);
    }

    //Used in testing
    protected void setGenericMapper(GenericAllToOneFederationToLocalMapper mapper) {
        this.genericMapper = mapper;
    }
}
