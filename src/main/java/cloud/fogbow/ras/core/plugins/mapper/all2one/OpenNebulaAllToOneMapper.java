package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.common.plugins.cloudidp.opennebula.OpenNebulaIdentityProviderPlugin;

import java.util.Map;

public class OpenNebulaAllToOneMapper extends GenericAllToOneSystemToCloudMapper {

    private OpenNebulaIdentityProviderPlugin identityProviderPlugin;

    public OpenNebulaAllToOneMapper(String confFile) {
        super(confFile);
        this.identityProviderPlugin = new OpenNebulaIdentityProviderPlugin();
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
