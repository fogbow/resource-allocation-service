package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenNebulaAllToOneMapper;

public class OpenNebulaOneToOneMapper extends GenericOneToOneFederationToLocalMapper {
    public OpenNebulaOneToOneMapper(String mapperConfFilePath) {
            super(new OpenNebulaAllToOneMapper(mapperConfFilePath));
    }
}
