package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.federationidentity.plugins.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.as.core.federationidentity.plugins.opennebula.OpenNebulaFederationIdentityProviderPlugin;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

public class OpenNebulaAllToOneMapper extends BasicAllToOneMapper implements FederationToLocalMapperPlugin {

    public OpenNebulaAllToOneMapper(String confFile) {
        super(confFile);
        String serviceUrl = super.getTokenGeneratorUrl();
        OpenNebulaClientFactory factory = new OpenNebulaClientFactory(serviceUrl);
        String provider = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.federationIdentityProviderPlugin = new OpenNebulaFederationIdentityProviderPlugin(factory, provider);
    }
}
