package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.CloudStackSystemUser;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.ras.core.plugins.mapper.all2one.CloudStackAllToOneMapper;

public class CloudStackOneToOneMapper extends GenericOneToOneSystemToCloudMapper<CloudStackUser, CloudStackSystemUser> {
    public CloudStackOneToOneMapper(String mapperConfFilePath) {
        super(new CloudStackAllToOneMapper(mapperConfFilePath));
    }
}

