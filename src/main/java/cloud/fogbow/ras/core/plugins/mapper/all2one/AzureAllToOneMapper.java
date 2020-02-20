package cloud.fogbow.ras.core.plugins.mapper.all2one;

import java.util.Map;

import cloud.fogbow.as.core.models.AzureSystemUser;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.plugins.cloudidp.azure.AzureIdentityProviderPlugin;
import com.google.common.annotations.VisibleForTesting;

public class AzureAllToOneMapper extends GenericAllToOneSystemToCloudMapper<AzureUser, AzureSystemUser> {

    private AzureIdentityProviderPlugin plugin;
    
    public AzureAllToOneMapper(String mapperConfFilePath) throws FatalErrorException {
        super(mapperConfFilePath);
        this.plugin = new AzureIdentityProviderPlugin();
    }

    @Override
    public AzureUser getCloudUser(Map<String, String> credentials) throws FogbowException {
        return this.plugin.getCloudUser(credentials);
    }

    @VisibleForTesting
    void setIdentityProviderPlugin(AzureIdentityProviderPlugin plugin) {
        this.plugin = plugin;
    }
}
