package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.models.CloudStackSystemUser;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.plugins.cloudidp.cloudstack.v4_9.CloudStackIdentityProviderPlugin;

import java.util.HashMap;
import java.util.Map;

public class CloudStackAllToOneMapper extends GenericAllToOneSystemToCloudMapper<CloudStackUser, CloudStackSystemUser> {
    private CloudStackIdentityProviderPlugin identityProviderPlugin;

    // Here we use HashMap instead of Map because RasClassFactory cannot
    // handle properly polymorphism in constructors.
    public CloudStackAllToOneMapper(String idpUrl, HashMap<String, String> credentials) {
        super(idpUrl, credentials);
        this.identityProviderPlugin = new CloudStackIdentityProviderPlugin(super.getIdpUrl());
    }
    
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
