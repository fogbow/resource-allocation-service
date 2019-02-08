package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.tokengenerator.plugins.cloudstack.CloudStackTokenGeneratorPlugin;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

public class CloudStackAllToOneMapper extends BasicAllToOneMapper implements FederationToLocalMapperPlugin {

    public CloudStackAllToOneMapper(String confFile) {
        super(confFile);
        String  timeoutStr = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                ConfigurationPropertyDefaults.HTTP_REQUEST_TIMEOUT);
        HttpRequestClientUtil client =  new HttpRequestClientUtil(new Integer(timeoutStr));
        String serviceUrl = super.getTokenGeneratorUrl();
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.tokenGeneratorPlugin = new CloudStackTokenGeneratorPlugin(client, serviceUrl, provider);
    }
}
