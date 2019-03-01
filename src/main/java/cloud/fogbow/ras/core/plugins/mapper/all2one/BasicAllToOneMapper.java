package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.federationidentity.FederationIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

import java.util.Map;

public class BasicAllToOneMapper extends GenericAllToOneFederationToLocalMapper
        implements FederationToLocalMapperPlugin {
    FederationIdentityProviderPlugin federationIdentityProviderPlugin;

    public BasicAllToOneMapper(String mapperConfFilePath) throws FatalErrorException {
        super(mapperConfFilePath);
    }

    @Override
    public FederationUser getFederationUser(Map<String, String> credentials) throws FogbowException {
        return this.federationIdentityProviderPlugin.getFederationUser(credentials);
    }

    @Override
    public CloudToken decorateToken(CloudToken token, FederationUser federationUser) throws UnexpectedException {
        return token;
    }

    public void setFederationIdentityProviderPlugin(FederationIdentityProviderPlugin federationIdentityProviderPlugin) {
        this.federationIdentityProviderPlugin = federationIdentityProviderPlugin;
    }
}
