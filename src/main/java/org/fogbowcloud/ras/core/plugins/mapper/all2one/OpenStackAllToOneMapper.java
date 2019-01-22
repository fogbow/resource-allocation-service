package org.fogbowcloud.ras.core.plugins.mapper.all2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.identity.openstack.v3.OpenStackIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;

public class OpenStackAllToOneMapper implements FederationToLocalMapperPlugin<OpenStackV3Token> {
    private GenericAllToOneFederationToLocalMapper genericMapper;

    public OpenStackAllToOneMapper(String interoperabilityConfFilePath, String mapperConfFilePath) {
        this.genericMapper = new GenericAllToOneFederationToLocalMapper(
                new OpenStackTokenGeneratorPlugin(interoperabilityConfFilePath),
                new OpenStackIdentityPlugin(), mapperConfFilePath);
    }

    @Override
    public OpenStackV3Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return (OpenStackV3Token) genericMapper.map(token);
    }

    //Used in testing
    protected void setGenericMapper(GenericAllToOneFederationToLocalMapper mapper) {
        this.genericMapper = mapper;
    }
}
