package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

public class GenericOneToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
    private FederationToLocalMapperPlugin remoteMapper;
    private String memberId;

    public GenericOneToOneFederationToLocalMapper(FederationToLocalMapperPlugin remoteMapper) {
        this.remoteMapper = remoteMapper;
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    @Override
    public CloudToken map(FederationUser token) throws FogbowException {
        if (token.getTokenProvider().equals(this.memberId)) {
            return new CloudToken(token.getTokenProvider(), token.getUserId(), token.getTokenValue());
        } else {
            return remoteMapper.map(token);
        }
    }

    public String getMemberId() {
        return this.memberId;
    }
}
