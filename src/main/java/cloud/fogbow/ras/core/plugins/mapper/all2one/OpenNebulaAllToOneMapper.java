package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.tokengenerator.plugins.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.as.core.tokengenerator.plugins.opennebula.OpenNebulaTokenGeneratorPlugin;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;

public class OpenNebulaAllToOneMapper extends BasicAllToOneMapper {
    public static final String OPENNEBULA_ENDPOINT = "opennebula_endpoint";

    public OpenNebulaAllToOneMapper(String confFile) {
        super(confFile);
        String endpoint = PropertiesHolder.getInstance().getProperty(OPENNEBULA_ENDPOINT);
        OpenNebulaClientFactory factory = new OpenNebulaClientFactory(endpoint);
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        this.tokenGeneratorPlugin = new OpenNebulaTokenGeneratorPlugin(factory, provider);
    }
}
