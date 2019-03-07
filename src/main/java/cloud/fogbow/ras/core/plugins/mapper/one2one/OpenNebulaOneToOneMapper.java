package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.OneToOneMappableSystemUser;
import cloud.fogbow.as.core.models.OpenNebulaSystemUser;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenNebulaAllToOneMapper;

public class OpenNebulaOneToOneMapper extends GenericOneToOneSystemToCloudMapper {
    public OpenNebulaOneToOneMapper(String mapperConfFilePath) {
            super(new OpenNebulaAllToOneMapper(mapperConfFilePath));
    }


    @Override
    public OpenNebulaUser localMap(OneToOneMappableSystemUser systemUser) throws InvalidParameterException {
        if (!(systemUser instanceof OpenNebulaSystemUser)) {
            throw new InvalidParameterException(Messages.Exception.SYSTEM_USER_TYPE_MISMATCH);
        }
        OpenNebulaSystemUser oneUser = (OpenNebulaSystemUser) systemUser;
        return new OpenNebulaUser(oneUser.getId(), oneUser.getName(), oneUser.getToken());
    }
}
