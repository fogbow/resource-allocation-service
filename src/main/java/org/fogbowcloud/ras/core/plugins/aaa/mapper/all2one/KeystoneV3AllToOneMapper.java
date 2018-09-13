package org.fogbowcloud.ras.core.plugins.aaa.mapper.all2one;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.KeystoneV3TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;

public class KeystoneV3AllToOneMapper implements FederationToLocalMapperPlugin {
    private GenericAllToOneFederationToLocalMapper genericMapper;

    public KeystoneV3AllToOneMapper() {
        this.genericMapper = new GenericAllToOneFederationToLocalMapper(new KeystoneV3TokenGeneratorPlugin(),
                new KeystoneV3IdentityPlugin(), "keystone-v3-mapper.conf");
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        String apiKey = "hvlpPU4Adtz7HBgO90gY9AAKWAjAy71LayerYe7M6D9NDD9RTJ0jzJnR8Mecdn9NXaS9EbogBJhNmAaec3m5oA";
        String secretKey = "BA3_UD42S6Y_oRlL18b-p7aumTNGCLk4FixJO6DSjXZLsPV9u9qRmNNEn3MGvVtdOfVDBcSqaDPnn96uEpoYRg";
        return new CloudStackToken(apiKey + ":" + secretKey);
//        return genericMapper.map(token);
    }

    //Used in testing
    protected void setGenericMapper(GenericAllToOneFederationToLocalMapper mapper) {
        this.genericMapper = mapper;
    }
}
