package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenNebulaAllToOneMapper;

public class OpenNebulaOneToOneMapper extends GenericOneToOneFederationToLocalMapper
        implements FederationToLocalMapperPlugin<CloudToken> {
    public OpenNebulaOneToOneMapper(String mapperConfFilePath) {
            super(new OpenNebulaAllToOneMapper(mapperConfFilePath));
    }
}
