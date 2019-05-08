package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.cloudidp.aws.AwsIdentityProviderPlugin;

import java.util.Map;

public class AwsAllToOneMapper extends GenericAllToOneSystemToCloudMapper<AwsUser, SystemUser> {

    private AwsIdentityProviderPlugin identityProviderPlugin;

    public AwsAllToOneMapper(String mapperConfFilePath) {
        super(mapperConfFilePath);
        identityProviderPlugin = new AwsIdentityProviderPlugin();
    }

    public AwsUser getCloudUser(Map<String , String> credentials) throws FogbowException {
        return identityProviderPlugin.getCloudUser(credentials);
    }
}
