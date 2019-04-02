package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.models.CloudStackSystemUser;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.plugins.cloudidp.cloudstack.CloudStackIdentityProviderPlugin;

import java.util.Map;

public class CloudStackAllToOneMapper extends GenericAllToOneSystemToCloudMapper<CloudStackUser, CloudStackSystemUser> {
    private CloudStackIdentityProviderPlugin identityProviderPlugin;

    public CloudStackAllToOneMapper(String confFile) {
        super(confFile);
        this.identityProviderPlugin = new CloudStackIdentityProviderPlugin(super.getIdpUrl());
    }

    @Override
    public CloudStackUser getCloudUser(Map<String, String> credentials) throws FogbowException {
        return this.identityProviderPlugin.getCloudUser(credentials);
    }

    // Used for testing only
    public void setIdentityProviderPlugin(CloudStackIdentityProviderPlugin identityProviderPlugin) {
        this.identityProviderPlugin = identityProviderPlugin;
    }
}
