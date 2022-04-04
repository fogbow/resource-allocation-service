package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;

import java.util.HashMap;
import java.util.Map;

public class EmulatedAllToOneMapper extends GenericAllToOneSystemToCloudMapper<CloudUser, SystemUser> {

    // Here we use HashMap instead of Map because RasClassFactory cannot
    // handle properly polymorphism in constructors.
    public EmulatedAllToOneMapper(String idpUrl, HashMap<String, String> credentials) {
        super(idpUrl, credentials);
    }
    
    public EmulatedAllToOneMapper(String mapperConfFilePath) {
        super(mapperConfFilePath);
    }

    public CloudUser getCloudUser(Map<String , String> credentials) {
        return new CloudUser("emulatedUserId", "emulatedUserName", "emulatedToken");
    }
}
