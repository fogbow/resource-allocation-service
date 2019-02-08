package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.tokengenerator.plugins.openstack.v3.OpenStackTokenGeneratorPlugin;
import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

import java.util.Map;

public class OpenStackAllToOneMapper extends BasicAllToOneMapper implements FederationToLocalMapperPlugin {

    public OpenStackAllToOneMapper(String confFile) {
        super(confFile);
        String  timeoutStr = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                ConfigurationPropertyDefaults.HTTP_REQUEST_TIMEOUT);
        HttpRequestClientUtil client =  new HttpRequestClientUtil(new Integer(timeoutStr));
        String serviceUrl = super.getTokenGeneratorUrl() + OpenStackConstants.Identity.V3_TOKENS_ENDPOINT_PATH;
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.tokenGeneratorPlugin = new OpenStackTokenGeneratorPlugin(client, serviceUrl, provider);
    }

    @Override
    public CloudToken decorateToken(CloudToken token, Map<String, String> attributes) {
        String projectId = attributes.get(OpenStackConstants.Identity.PROJECT_KEY_JSON);
        String provider = token.getTokenProviderId();
        String userId = token.getUserId();
        String tokenValue = token.getTokenValue();
        OpenStackV3Token decoratedToken = new OpenStackV3Token(provider, userId, tokenValue, projectId);
        return decoratedToken;
    }
}
