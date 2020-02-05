package cloud.fogbow.ras.core.plugins.mapper.all2one;

import java.util.Map;

import cloud.fogbow.as.core.models.AzureV1SystemUser;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureV1User;
import cloud.fogbow.common.plugins.cloudidp.azure.v1.AzureV1IdentityProviderPlugin;

public class AzureV1AllToOneMapper extends GenericAllToOneSystemToCloudMapper<AzureV1User, AzureV1SystemUser> {

    private AzureV1IdentityProviderPlugin plugin;
    
    public AzureV1AllToOneMapper(String mapperConfFilePath) throws FatalErrorException {
        super(mapperConfFilePath);
        this.plugin = new AzureV1IdentityProviderPlugin();
    }

    @Override
    public AzureV1User getCloudUser(Map<String, String> credentials) throws FogbowException {
        return this.plugin.getCloudUser(credentials);
    }

}
