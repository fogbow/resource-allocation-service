package org.fogbowcloud.manager.core.plugins.behavior.mapper.one2one;

import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;

public class GenericOneToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
    private FederationToLocalMapperPlugin remoteMapper;
    private String memberId;

    public GenericOneToOneFederationToLocalMapper(FederationToLocalMapperPlugin remoteMapper) {
        this.remoteMapper = remoteMapper;
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowManagerException {
        if (token.getTokenProvider().equals(this.memberId)) {
            return token;
        } else {
            return remoteMapper.map(token);
        }
    }
}
