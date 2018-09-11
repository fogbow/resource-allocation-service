package org.fogbowcloud.ras.core.plugins.aaa.mapper.one2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.all2one.CloudStackAllToOneMapper;

public class CloudStackOneToOneMapper implements FederationToLocalMapperPlugin {
    private GenericOneToOneFederationToLocalMapper genericMapper;

    public CloudStackOneToOneMapper() {
        this.genericMapper = new GenericOneToOneFederationToLocalMapper(new CloudStackAllToOneMapper());
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return this.genericMapper.map(token);
    }
}

