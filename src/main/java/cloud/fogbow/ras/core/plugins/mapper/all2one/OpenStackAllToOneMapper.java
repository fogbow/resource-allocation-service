package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.federationidentity.plugins.openstack.v3.OpenStackFederationIdentityProviderPlugin;
import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

import java.util.Map;

public class OpenStackAllToOneMapper extends BasicAllToOneMapper implements FederationToLocalMapperPlugin {

    public OpenStackAllToOneMapper(String confFile) {
        super(confFile);
        HttpRequestClientUtil client =  new HttpRequestClientUtil();
        String serviceUrl = super.getTokenGeneratorUrl() + OpenStackConstants.Identity.V3_TOKENS_ENDPOINT_PATH;
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.federationIdentityProviderPlugin = new OpenStackFederationIdentityProviderPlugin(client, serviceUrl, provider);
    }

    @Override
    public CloudToken decorateToken(CloudToken token, FederationUser federationUser) throws UnexpectedException {
        OpenStackV3Token decoratedToken = new OpenStackV3Token(federationUser);
        return decoratedToken;
    }
}
