package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import org.apache.log4j.Logger;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.VolumePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OneConfigurationConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4.CreateVolumeRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.volume.v2.GetVolumeResponse;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;

import java.io.File;
import java.util.Properties;

public class OpenNebulaVolumePlugin implements VolumePlugin<Token> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);

    public static final String OPENNEBULA_DATABLOCK_IMAGE_TYPE = "DATABLOCK";
    public static final String OPENNEBULA_RAW_FSTYPE = "raw";
    public static final String OPENNEBULA_BLOCK_DISK_TYPE = "BLOCK";
    public static final String OPENNEBULA_DATASTORE_DEFAULT_DEVICE_PREFIX = "vd";
    public static final String OPENNEBULA_PERSISTENT_DISK_YES = "YES";

    private String openNebulaEndpoint;
    private Integer dataStoreId;
    private String devicePrefix;
    private OpenNebulaClientFactory factory;

    Properties properties;

    public OpenNebulaVolumePlugin() {
        super();
        factory = new OpenNebulaClientFactory();

        String filePath = HomeDir.getPath() + File.separator + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;
        properties = PropertiesUtil.readProperties(filePath);

        String dataStoreIdStr = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_DATASTORE_ID);
        dataStoreId = dataStoreIdStr == null ? null: Integer.valueOf(dataStoreIdStr);
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        String volumeName = volumeOrder.getName();
        int volumeSize = volumeOrder.getVolumeSize();

        Client client = factory.createClient(localUserAttributes.getTokenValue());

        CreateVolumeRequest request = new CreateVolumeRequest.Builder()
                .name(volumeName)
                .size(volumeSize)
                .persistent(OPENNEBULA_PERSISTENT_DISK_YES)
                .type(OPENNEBULA_DATABLOCK_IMAGE_TYPE)
                .fileSystemType(OPENNEBULA_RAW_FSTYPE)
                .diskType(OPENNEBULA_BLOCK_DISK_TYPE)
                .devicePrefix(OPENNEBULA_DATASTORE_DEFAULT_DEVICE_PREFIX)
                .build();

        String volumeTemplate = request.getVolumeImageRequestTemplate().generateTemplate();

//        There is no need for logging this in the new fogbow
//        LOGGER.info("Creating datablock image with template: " + volumeTemplate);

        

        return factory.allocateImage(client, volumeTemplate, dataStoreId);
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String volumeInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {

    }
}