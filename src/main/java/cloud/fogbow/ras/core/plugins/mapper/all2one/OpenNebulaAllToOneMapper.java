package cloud.fogbow.ras.core.plugins.mapper.all2one;

import java.util.Map;
import java.util.Properties;

import cloud.fogbow.as.constants.ConfigurationPropertyKeys;
import cloud.fogbow.as.core.models.OpenNebulaSystemUser;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.common.plugins.cloudidp.opennebula.v5_4.OpenNebulaIdentityProviderPlugin;
import cloud.fogbow.common.util.PropertiesUtil;

public class OpenNebulaAllToOneMapper extends GenericAllToOneSystemToCloudMapper<OpenNebulaUser, OpenNebulaSystemUser> {

    private OpenNebulaIdentityProviderPlugin identityProviderPlugin;

    public OpenNebulaAllToOneMapper(String mapperConfFilePath) {
        super(mapperConfFilePath);
        Properties properties = PropertiesUtil.readProperties(mapperConfFilePath);
        String identityUrl = properties.getProperty(ConfigurationPropertyKeys.CLOUD_IDENTITY_PROVIDER_URL_KEY);
        this.identityProviderPlugin = new OpenNebulaIdentityProviderPlugin(identityUrl);
    }

    @Override
    public OpenNebulaUser getCloudUser(Map<String, String> credentials) throws FogbowException {
        return this.identityProviderPlugin.getCloudUser(credentials);
    }

    // Used only in tests
    public void setIdentityProviderPlugin(OpenNebulaIdentityProviderPlugin identityProviderPlugin) {
        this.identityProviderPlugin = identityProviderPlugin;
    }
}
