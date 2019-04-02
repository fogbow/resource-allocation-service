package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.OpenStackV3ScopedSystemUser;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenStackAllToOneMapper;

public class OpenStackOneToOneMapper extends GenericOneToOneSystemToCloudMapper<OpenStackV3User, OpenStackV3ScopedSystemUser> {
    public OpenStackOneToOneMapper(String mapperConfFilePath) {
        super(new OpenStackAllToOneMapper(mapperConfFilePath));
    }
}
