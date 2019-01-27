package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.tokengenerator.plugins.cloudstack.CloudStackTokenGeneratorPlugin;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;

public class CloudStackAllToOneMapper extends BasicAllToOneMapper {
    public static final String CLOUDSTACK_ENDPOINT = "cloudstack_endpoint";

    public CloudStackAllToOneMapper(String confFile) {
        super(confFile);
        String  timeoutStr = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT_KEY);
        HttpRequestClientUtil client =  new HttpRequestClientUtil(new Integer(timeoutStr));
        String endpoint = PropertiesHolder.getInstance().getProperty(CLOUDSTACK_ENDPOINT);
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID_KEY);
        this.tokenGeneratorPlugin = new CloudStackTokenGeneratorPlugin(client, endpoint, provider);
    }
}
