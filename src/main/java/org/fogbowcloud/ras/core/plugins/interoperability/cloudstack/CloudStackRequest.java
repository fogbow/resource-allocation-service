package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.io.File;
import java.util.Properties;

public abstract class CloudStackRequest {
    private URIBuilder uriBuilder;

    protected CloudStackRequest() throws InvalidParameterException {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);

        String baseEndpoint = properties.getProperty(CloudStackTokenGeneratorPlugin.CLOUDSTACK_URL);
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
