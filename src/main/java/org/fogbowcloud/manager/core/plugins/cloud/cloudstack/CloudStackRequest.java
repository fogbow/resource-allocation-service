package org.fogbowcloud.manager.core.plugins.cloud.cloudstack;

import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;

import java.io.File;
import java.util.Properties;

public abstract class CloudStackRequest {

    private static final String CLOUDSTACK_URL_KEY = "cloudstack_api_url";

    private URIBuilder uriBuilder;

    protected CloudStackRequest(String endpoint, String command) throws InvalidParameterException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);

        String baseEndpoint = properties.getProperty(CLOUDSTACK_URL_KEY);
        this.uriBuilder = CloudStackUrlUtil.createURIBuilder(baseEndpoint, getCommand());
    }

    protected void addParameter(String parameter, String value) {
        if (value != null) {
            uriBuilder.addParameter(parameter, value);
        }
    }

    public URIBuilder getUriBuilder() {
        return uriBuilder;
    }

    public abstract String getCommand();
}
