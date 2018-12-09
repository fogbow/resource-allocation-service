package org.fogbowcloud.ras.core.plugins.mapper.one2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.mapper.all2one.OpenStackAllToOneMapper;

public class OpenStackOneToOneMapper implements FederationToLocalMapperPlugin<OpenStackV3Token> {
    private GenericOneToOneFederationToLocalMapper genericMapper;

    public OpenStackOneToOneMapper(String interoperabilityConfFilePath, String mapperConfFilePath) {
        this.genericMapper = new GenericOneToOneFederationToLocalMapper(
                new OpenStackAllToOneMapper(interoperabilityConfFilePath, mapperConfFilePath));
    }

    @Override
    public OpenStackV3Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return (OpenStackV3Token) this.genericMapper.map(token);
    }
}
