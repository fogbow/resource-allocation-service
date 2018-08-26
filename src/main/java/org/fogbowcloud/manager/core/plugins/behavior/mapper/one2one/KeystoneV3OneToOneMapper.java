package org.fogbowcloud.manager.core.plugins.behavior.mapper.one2one;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.all2one.KeystoneV3AllToOneMapper;

public class KeystoneV3OneToOneMapper implements FederationToLocalMapperPlugin {
    private GenericOneToOneFederationToLocalMapper genericMapper;

    public KeystoneV3OneToOneMapper() {
        this.genericMapper = new GenericOneToOneFederationToLocalMapper(new KeystoneV3AllToOneMapper());
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowManagerException {
        return this.genericMapper.map(token);
    }
}
