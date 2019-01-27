package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.ras.core.plugins.mapper.all2one.CloudStackAllToOneMapper;

public class CloudStackOneToOneMapper extends GenericOneToOneFederationToLocalMapper {
    public CloudStackOneToOneMapper(String mapperConfFilePath) {
        super(new CloudStackAllToOneMapper(mapperConfFilePath));
    }
}

