package cloud.fogbow.ras.core.plugins.mapper;

import cloud.fogbow.as.core.models.OneToOneMappableSystemUser;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;

public interface SystemToCloudMapperPlugin<T extends CloudUser> {
    public <S extends SystemUser & OneToOneMappableSystemUser> T map(S systemUser) throws FogbowException;
}

