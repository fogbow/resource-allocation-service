package org.fogbowcloud.ras.core.plugins.aaa.authorization.service;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.util.Properties;

public class PrePaidAuthorizationPluginClient extends DistributedAuthorizationPluginClient {
    private static final String CONF_FILE = "prepaid-authorization-plugin-client.conf";
    private static final String PROP_SERVER_URL = "server_url";

    public PrePaidAuthorizationPluginClient() {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() + CONF_FILE);
        String apiUrl = properties.getProperty(PROP_SERVER_URL);
        super.setServerUrl(apiUrl);
    }
}
