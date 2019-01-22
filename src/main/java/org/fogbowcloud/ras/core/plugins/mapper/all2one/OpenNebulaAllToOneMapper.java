package org.fogbowcloud.ras.core.plugins.mapper.all2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula.OpenNebulaTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.opennebula.OpenNebulaIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

public class OpenNebulaAllToOneMapper implements FederationToLocalMapperPlugin {
    private GenericAllToOneFederationToLocalMapper genericMapper;

    public OpenNebulaAllToOneMapper(String interoperabilityConfFilePath, String mapperConfFilePath) {
        this.genericMapper = new GenericAllToOneFederationToLocalMapper(
                new OpenNebulaTokenGeneratorPlugin(interoperabilityConfFilePath),
                new OpenNebulaIdentityPlugin(), mapperConfFilePath);
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        return this.genericMapper.map(token);
    }
}
