package org.fogbowcloud.ras.core.plugins.aaa.mapper.one2one;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;

public class GenericOneToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
    private FederationToLocalMapperPlugin remoteMapper;
    private String memberId;

    public GenericOneToOneFederationToLocalMapper(FederationToLocalMapperPlugin remoteMapper) {
        this.remoteMapper = remoteMapper;
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    @Override
    public Token map(FederationUserToken token) throws UnexpectedException, FogbowRasException {
        if (token.getTokenProvider().equals(this.memberId)) {
            return token;
        } else {
            return remoteMapper.map(token);
        }
    }
}
