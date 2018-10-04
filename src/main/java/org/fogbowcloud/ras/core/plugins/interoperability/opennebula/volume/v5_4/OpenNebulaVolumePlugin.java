package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import org.apache.log4j.Logger;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.VolumePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OneConfigurationConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4.CreateVolumeRequest;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import javax.lang.model.type.ErrorType;
import java.io.File;
import java.util.Properties;

public class OpenNebulaVolumePlugin implements VolumePlugin<Token> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);

    private Integer dataStoreId;
    private OpenNebulaClientFactory factory;

    Properties properties;

    public OpenNebulaVolumePlugin() {
        super();
        this.factory = new OpenNebulaClientFactory();

        String filePath = HomeDir.getPath() + File.separator + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME;
        properties = PropertiesUtil.readProperties(filePath);

        String dataStoreIdStr = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_DATASTORE_ID);
        dataStoreId = dataStoreIdStr == null ? null: Integer.valueOf(dataStoreIdStr);
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        String volumeName = volumeOrder.getName();
        int volumeSize = volumeOrder.getVolumeSize();

        Client oneClient = factory.createClient(localUserAttributes.getTokenValue());

        CreateVolumeRequest request = new CreateVolumeRequest.Builder()
                .name(volumeName)
                .size(volumeSize)
                .persistent( OpenNebulaXmlTagsConstants.VirtualMachine.OPENNEBULA_PERSISTENT_DISK_YES )
                .type( OpenNebulaXmlTagsConstants.VirtualMachine.OPENNEBULA_DATABLOCK_IMAGE_TYPE )
                .fileSystemType( OpenNebulaXmlTagsConstants.VirtualMachine.OPENNEBULA_RAW_FSTYPE )
                .diskType( OpenNebulaXmlTagsConstants.VirtualMachine.OPENNEBULA_BLOCK_DISK_TYPE )
                .devicePrefix( OpenNebulaXmlTagsConstants.VirtualMachine.OPENNEBULA_DATASTORE_DEFAULT_DEVICE_PREFIX )
                .build();

        String volumeTemplate = request.getVolumeImageRequestTemplate().generateTemplate();

        return this.factory.allocateImage(oneClient, volumeTemplate, dataStoreId);
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        Client oneClient  = this.factory.createClient(localUserAttributes.getTokenValue());

        ImagePool imagePool = this.factory.createImagePool(oneClient);

        Image oneImage = imagePool.getById(Integer.parseInt(volumeInstanceId));

        int imageSize = Integer.parseInt(oneImage.xpath(OpenNebulaXmlTagsConstants.VirtualMachine.SIZE));

        String instanceName = oneImage.getName();

        InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.VOLUME, oneImage.stateString());

        return new VolumeInstance(volumeInstanceId, instanceState, instanceName, imageSize);
    }

    @Override
    public void deleteInstance(String volumeInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        Client oneClient = this.factory.createClient(localUserAttributes.getTokenValue());
        ImagePool imagePool = this.factory.createImagePool(oneClient);
        Image oneImage = imagePool.getById(Integer.parseInt(volumeInstanceId));

        OneResponse response = oneImage.delete();

        if(response.isError()){

            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_INSTANCE, volumeInstanceId));
            throw new UnexpectedException(response.getMessage());
        }
    }
}