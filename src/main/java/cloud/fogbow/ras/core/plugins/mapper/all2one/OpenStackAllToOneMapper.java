package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.PropertiesHolder;
import cloud.fogbow.as.core.tokengenerator.plugins.openstack.v3.OpenStackTokenGeneratorPlugin;
import cloud.fogbow.common.constants.OpenStackRestApiConstants;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;

import java.util.Map;

public class OpenStackAllToOneMapper extends BasicAllToOneMapper {
    public static final String OPENSTACK_KEYSTONE_V3_ENDPOINT = "openstack_keystone_v3_endpoint";

    public OpenStackAllToOneMapper(String confFile) {
        super(confFile);
        String  timeoutStr = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT_KEY);
        HttpRequestClientUtil client =  new HttpRequestClientUtil(new Integer(timeoutStr));
        String endpoint = PropertiesHolder.getInstance().getProperty(OPENSTACK_KEYSTONE_V3_ENDPOINT)
                + OpenStackRestApiConstants.Identity.V3_TOKENS_ENDPOINT_PATH;
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID_KEY);
        this.tokenGeneratorPlugin = new OpenStackTokenGeneratorPlugin(client, endpoint, provider);
    }

    @Override
    public CloudToken decorateToken(CloudToken token, Map<String, String> attributes) {
        String projectId = attributes.get(OpenStackRestApiConstants.Identity.PROJECT_KEY_JSON);
        String provider = token.getTokenProviderId();
        String userId = token.getUserId();
        String tokenValue = token.getTokenValue();
        OpenStackV3Token decoratedToken = new OpenStackV3Token(provider, userId, tokenValue, projectId);
        return decoratedToken;
    }
}
