package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.CloudStackSystemUser;
import cloud.fogbow.as.core.models.OneToOneMappableSystemUser;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.mapper.all2one.CloudStackAllToOneMapper;

public class CloudStackOneToOneMapper extends GenericOneToOneSystemToCloudMapper {
    public CloudStackOneToOneMapper(String mapperConfFilePath) {
        super(new CloudStackAllToOneMapper(mapperConfFilePath));
    }
}

