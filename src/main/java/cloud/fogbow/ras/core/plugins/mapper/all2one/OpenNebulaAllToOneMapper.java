package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.tokengenerator.plugins.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.as.core.tokengenerator.plugins.opennebula.OpenNebulaTokenGeneratorPlugin;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

public class OpenNebulaAllToOneMapper extends BasicAllToOneMapper implements FederationToLocalMapperPlugin {

    public OpenNebulaAllToOneMapper(String confFile) {
        super(confFile);
        String serviceUrl = super.getTokenGeneratorUrl();
        OpenNebulaClientFactory factory = new OpenNebulaClientFactory(serviceUrl);
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID_KEY);
        this.tokenGeneratorPlugin = new OpenNebulaTokenGeneratorPlugin(factory, provider);
    }
}
