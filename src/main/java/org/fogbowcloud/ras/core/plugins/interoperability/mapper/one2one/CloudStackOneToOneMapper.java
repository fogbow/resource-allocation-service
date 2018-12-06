package org.fogbowcloud.ras.core.plugins.interoperability.mapper.one2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.mapper.all2one.CloudStackAllToOneMapper;

public class CloudStackOneToOneMapper implements FederationToLocalMapperPlugin<CloudStackToken> {
    private GenericOneToOneFederationToLocalMapper genericMapper;

    public CloudStackOneToOneMapper(String interoperabilityConfFilePath, String mapperConfFilePath) {
        this.genericMapper = new GenericOneToOneFederationToLocalMapper(
                new CloudStackAllToOneMapper(interoperabilityConfFilePath, mapperConfFilePath));
    }

    @Override
    public CloudStackToken map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return (CloudStackToken) this.genericMapper.map(token);
    }
}

