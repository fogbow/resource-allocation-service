package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

public class GenericOneToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
    private FederationToLocalMapperPlugin remoteMapper;
    private String memberId;

    public GenericOneToOneFederationToLocalMapper(FederationToLocalMapperPlugin remoteMapper) {
        this.remoteMapper = remoteMapper;
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
    }

    @Override
    public CloudToken map(FederationUser federationUser) throws FogbowException {
        if (federationUser.getTokenProviderId().equals(this.memberId)) {
            return new CloudToken(federationUser);
        } else {
            return remoteMapper.map(federationUser);
        }
    }

    public String getMemberId() {
        return this.memberId;
    }

    protected void setRemoteMapper(FederationToLocalMapperPlugin remoteMapper) {
        this.remoteMapper = remoteMapper;
    }
}
