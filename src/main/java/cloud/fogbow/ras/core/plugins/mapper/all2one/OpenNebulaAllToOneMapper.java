package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.tokengenerator.plugins.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.as.core.tokengenerator.plugins.opennebula.OpenNebulaTokenGeneratorPlugin;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

public class OpenNebulaAllToOneMapper extends BasicAllToOneMapper implements FederationToLocalMapperPlugin {
    public static final String OPENNEBULA_ENDPOINT = "opennebula_endpoint";

    public OpenNebulaAllToOneMapper(String confFile) {
        super(confFile);
        String endpoint = PropertiesHolder.getInstance().getProperty(OPENNEBULA_ENDPOINT);
        OpenNebulaClientFactory factory = new OpenNebulaClientFactory(endpoint);
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID_KEY);
        this.tokenGeneratorPlugin = new OpenNebulaTokenGeneratorPlugin(factory, provider);
    }
}
