package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import cloud.fogbow.ras.core.plugins.mapper.all2one.CloudStackAllToOneMapper;

public class CloudStackOneToOneMapper extends GenericOneToOneFederationToLocalMapper
        implements FederationToLocalMapperPlugin<CloudToken> {
    public CloudStackOneToOneMapper(String mapperConfFilePath) {
        super(new CloudStackAllToOneMapper(mapperConfFilePath));
    }
}

