package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;

import java.util.Map;

public class EmulatedAllToOneMapper extends GenericAllToOneSystemToCloudMapper<CloudUser, SystemUser> {

    public EmulatedAllToOneMapper(String mapperConfFilePath) {
        super(mapperConfFilePath);
    }

    public CloudUser getCloudUser(Map<String , String> credentials) {
        return new CloudUser("emulatedUserId", "emulatedUserName", "emulatedToken");
    }
}
