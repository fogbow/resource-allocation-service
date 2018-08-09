package org.fogbowcloud.manager.core.plugins.cloud.cloudstack;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudStackHelper;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;

import java.io.File;
import java.util.Properties;

public class CloudStackComputePlugin implements ComputePlugin {

    private static final Logger LOGGER = Logger.getLogger(CloudStackComputePlugin.class);

    private static final String CLOUDSTACK_URL_KEY = "cloudstack_api_url";
    protected static final String LIST_VMS_COMMAND = "listVirtualMachines";
    protected static final String VIRTUAL_MACHINE_ID = "id";

    private Properties properties;
    private HttpRequestClientUtil client;
    private String endpoint;

    public CloudStackComputePlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);

        this.endpoint = properties.getProperty(CLOUDSTACK_URL_KEY);
        initClient();
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        return "";

    }

    @Override
    public ComputeInstance getInstance(String computeInstanceId, Token localToken)
            throws FogbowManagerException {
        LOGGER.info("Getting instance " + computeInstanceId + " with tokens " + localToken);

        URIBuilder uriBuilder = CloudStackHelper.createURIBuilder(this.endpoint, LIST_VMS_COMMAND);
        uriBuilder.addParameter(VIRTUAL_MACHINE_ID, computeInstanceId);
        CloudStackHelper.sign(uriBuilder, localToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(uriBuilder.toString(), localToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        LOGGER.debug("Getting instance from json: " + jsonResponse);
        //ComputeInstance computeInstance = getInstanceFromJson(jsonResponse, localToken);

        return new ComputeInstance(computeInstanceId);

    }

    @Override
    public void deleteInstance(String computeInstanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {

    }

    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }
}
