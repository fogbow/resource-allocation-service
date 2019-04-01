package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.OneToOneMappableSystemUser;
import cloud.fogbow.as.core.models.OpenStackV3SystemUser;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenStackAllToOneMapper;

public class OpenStackOneToOneMapper extends GenericOneToOneSystemToCloudMapper {
    public OpenStackOneToOneMapper(String mapperConfFilePath) {
        super(new OpenStackAllToOneMapper(mapperConfFilePath));
    }
}
