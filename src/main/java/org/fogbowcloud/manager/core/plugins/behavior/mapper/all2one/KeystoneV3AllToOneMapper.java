package org.fogbowcloud.manager.core.plugins.behavior.mapper.all2one;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3.KeystoneV3TokenGeneratorPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

public class KeystoneV3AllToOneMapper implements FederationToLocalMapperPlugin {
    private GenericAllToOneFederationToLocalMapper genericMapper;

    public KeystoneV3AllToOneMapper() {
        this.genericMapper = new GenericAllToOneFederationToLocalMapper(new KeystoneV3TokenGeneratorPlugin(),
                new KeystoneV3IdentityPlugin(), "keystone-v3-mapper.conf");
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowManagerException {
        return genericMapper.map(token);
    }

    //Used in testing
    protected void setGenericMapper(GenericAllToOneFederationToLocalMapper mapper) {
        this.genericMapper = mapper;
    }
}
