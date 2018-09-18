package org.fogbowcloud.ras.core.plugins.aaa.mapper.all2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.identity.openstack.v3.OpenStackIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;

public class OpenStackAllToOneMapper implements FederationToLocalMapperPlugin {
    private GenericAllToOneFederationToLocalMapper genericMapper;

    public OpenStackAllToOneMapper() {
        this.genericMapper = new GenericAllToOneFederationToLocalMapper(new OpenStackTokenGeneratorPlugin(),
                new OpenStackIdentityPlugin(), "keystone-v3-mapper.conf");
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
