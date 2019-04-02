package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.OneToOneMappableSystemUser;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;

public abstract class GenericOneToOneSystemToCloudMapper<T extends CloudUser, S extends SystemUser & OneToOneMappableSystemUser>
        implements SystemToCloudMapperPlugin<T, S> {

    private SystemToCloudMapperPlugin remoteMapper;
    private String memberId;

    public GenericOneToOneSystemToCloudMapper(SystemToCloudMapperPlugin remoteMapper) {
        this.remoteMapper = remoteMapper;
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
    }

    @Override
    public T map(S systemUser) throws FogbowException {
        if (systemUser.getIdentityProviderId().equals(this.memberId)) {
            // TODO: check if token hasn't expired; if it has, then renew it.
            return (T) systemUser.generateCloudUser();
        } else {
            return (T) remoteMapper.map(systemUser);
        }
    }

    public SystemToCloudMapperPlugin getRemoteMapper() {
        return remoteMapper;
    }

    public String getMemberId() {
        return this.memberId;
    }

    protected void setRemoteMapper(SystemToCloudMapperPlugin remoteMapper) {
        this.remoteMapper = remoteMapper;
    }
}
