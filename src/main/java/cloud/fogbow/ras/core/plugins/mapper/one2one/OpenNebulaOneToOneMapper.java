package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.OpenNebulaSystemUser;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenNebulaAllToOneMapper;

public class OpenNebulaOneToOneMapper extends GenericOneToOneSystemToCloudMapper<OpenNebulaUser, OpenNebulaSystemUser> {
    public OpenNebulaOneToOneMapper(String mapperConfFilePath) {
            super(new OpenNebulaAllToOneMapper(mapperConfFilePath));
    }
}
