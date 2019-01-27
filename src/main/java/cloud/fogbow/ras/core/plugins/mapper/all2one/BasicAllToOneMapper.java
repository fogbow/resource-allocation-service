package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.tokengenerator.TokenGeneratorPlugin;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.AuthenticationUtil;

import java.util.Map;

public class BasicAllToOneMapper extends GenericAllToOneFederationToLocalMapper {
    TokenGeneratorPlugin tokenGeneratorPlugin;

    public BasicAllToOneMapper(String mapperConfFilePath) throws FatalErrorException {
        super(mapperConfFilePath);
    }

    @Override
    public Map<String, String> getAttributesString(Map<String, String> credentials) throws FogbowException {
        return AuthenticationUtil.getAttributes(this.tokenGeneratorPlugin.createTokenValue(credentials));
    }

    @Override
    public CloudToken decorateToken(CloudToken token, Map<String, String> attributes) {
        return token;
    }
}
