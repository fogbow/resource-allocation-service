package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.models.OpenStackV3SystemUser;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.plugins.cloudidp.openstack.v3.OpenStackIdentityProviderPlugin;

import java.util.HashMap;
import java.util.Map;

public class OpenStackAllToOneMapper extends GenericAllToOneSystemToCloudMapper<OpenStackV3User, OpenStackV3SystemUser> {
    private OpenStackIdentityProviderPlugin identityProviderPlugin;

    // Here we use HashMap instead of Map because RasClassFactory cannot
    // handle properly polymorphism in constructors.
    public OpenStackAllToOneMapper(String idpUrl, HashMap<String, String> credentials) {
        super(idpUrl, credentials);
        this.identityProviderPlugin = new OpenStackIdentityProviderPlugin(super.getIdpUrl());
    }
    
    public OpenStackAllToOneMapper(String confFile) {
        super(confFile);
        this.identityProviderPlugin = new OpenStackIdentityProviderPlugin(super.getIdpUrl());
    }

    @Override
    public OpenStackV3User getCloudUser(Map<String, String> credentials) throws FogbowException {
        return this.identityProviderPlugin.getCloudUser(credentials);
    }

    // Used only in tests
    public void setIdentityProviderPlugin(OpenStackIdentityProviderPlugin identityProviderPlugin) {
        this.identityProviderPlugin = identityProviderPlugin;
    }
}
