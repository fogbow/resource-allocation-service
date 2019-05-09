package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.cloudidp.aws.v2.AwsV2IdentityProviderPlugin;

import java.util.Map;

public class AwsV2AllToOneMapper extends GenericAllToOneSystemToCloudMapper<AwsV2User, SystemUser> {

    private AwsV2IdentityProviderPlugin identityProviderPlugin;

    public AwsV2AllToOneMapper(String mapperConfFilePath) {
        super(mapperConfFilePath);
        identityProviderPlugin = new AwsV2IdentityProviderPlugin();
    }

    public AwsV2User getCloudUser(Map<String , String> credentials) throws FogbowException {
        return identityProviderPlugin.getCloudUser(credentials);
    }
}
