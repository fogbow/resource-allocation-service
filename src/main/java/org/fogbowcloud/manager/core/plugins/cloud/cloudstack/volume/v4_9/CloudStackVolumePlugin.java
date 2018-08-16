package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume;

import java.io.File;
import java.util.Properties;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudStackHelper;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;

public class CloudStackVolumePlugin implements VolumePlugin<Token>{

    private static final Logger LOGGER = Logger.getLogger(CloudStackVolumePlugin.class);
    
    private   static final String CLOUDSTACK_ZONE_ID = "cloudstack_zone_id";
    protected static final String COMMAND = "command";
    protected static final String CREATE_VOLUME_COMMAND = "createVolume";
    protected static final String DISK_OFFERING_ID = "diskofferingid";
    protected static final String VOLUME_NAME = "name";
    private   static final String VOLUME_URL_KEY = "cloudstack_api_url";
    protected static final String ZONE_ID = "zoneid";
    protected static final String VOLUME_SIZE = "size";

    private static final String LIST_DISK_OFFERINGS_COMMAND = null;
    
    private HttpRequestClientUtil client;
    private String endpoint;
    private String zoneId;
    
    public CloudStackVolumePlugin() {
        HomeDir homeDir = HomeDir.getInstance();
        String filePath = homeDir.getPath() + File.separator + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(filePath);

        this.endpoint = properties.getProperty(VOLUME_URL_KEY);
        this.zoneId = properties.getProperty(CLOUDSTACK_ZONE_ID);
        
        initClient();
    }
    
    @Override
    public String requestInstance(VolumeOrder volumeOrder, Token localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        
        LOGGER.debug("Requesting volume instance with token " + localUserAttributes);
        
        String name = volumeOrder.getVolumeName();

        URIBuilder uriBuilder = CloudStackHelper.createURIBuilder(endpoint, CREATE_VOLUME_COMMAND);
        uriBuilder.addParameter(ZONE_ID, zoneId);
        uriBuilder.addParameter(VOLUME_NAME, name);
        
        getDiskOffering(volumeOrder, localUserAttributes, uriBuilder);
        
        CloudStackHelper.sign(uriBuilder, localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(uriBuilder.toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        
        LOGGER.debug("Getting instance from json: " + jsonResponse);
        return jsonResponse;
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, Token localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteInstance(String volumeInstanceId, Token localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        // TODO Auto-generated method stub
        
    }

    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }
    
    private void getDiskOffering(VolumeOrder volumeOrder, Token localUserAttributes,
            URIBuilder uriBuilder) {
        // TODO Auto-generated method stub
        
    }
    
}
