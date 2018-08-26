package org.fogbowcloud.ras.core.plugins.aaa.mapper.one2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.all2one.KeystoneV3AllToOneMapper;

public class KeystoneV3OneToOneMapper implements FederationToLocalMapperPlugin {
    private GenericOneToOneFederationToLocalMapper genericMapper;

    public KeystoneV3OneToOneMapper() {
        this.genericMapper = new GenericOneToOneFederationToLocalMapper(new KeystoneV3AllToOneMapper());
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return this.genericMapper.map(token);
    }
}
