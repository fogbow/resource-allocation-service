package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.cloudidp.aws.v2.AwsIdentityProviderPlugin;

import java.util.HashMap;
import java.util.Map;

public class AwsV2AllToOneMapper extends GenericAllToOneSystemToCloudMapper<AwsV2User, SystemUser> {

    private AwsIdentityProviderPlugin identityProviderPlugin;

    // Here we use HashMap instead of Map because RasClassFactory cannot
    // handle properly polymorphism in constructors.
    public AwsV2AllToOneMapper(String idpUrl, HashMap<String, String> credentials) {
        super(idpUrl, credentials);
        identityProviderPlugin = new AwsIdentityProviderPlugin();
    }
    
    public AwsV2AllToOneMapper(String mapperConfFilePath) {
        super(mapperConfFilePath);
        identityProviderPlugin = new AwsIdentityProviderPlugin();
    }

    public AwsV2User getCloudUser(Map<String , String> credentials) throws FogbowException {
        return identityProviderPlugin.getCloudUser(credentials);
    }
}
