package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.io.File;
import java.net.URI;
import java.util.Properties;

public abstract class CloudStackRequest {
    private URIBuilder uriBuilder;

    protected CloudStackRequest() {}

    protected CloudStackRequest(String baseEndpoint) throws InvalidParameterException {
        this.uriBuilder = CloudStackUrlUtil.createURIBuilder(baseEndpoint, getCommand());
    }

    protected void addParameter(String parameter, String value) {
        if (value != null) {
            this.uriBuilder.addParameter(parameter, value);
        }
    }

    public URIBuilder getUriBuilder() throws InvalidParameterException {
        return this.uriBuilder;
    }

    public abstract String getCommand();
}
