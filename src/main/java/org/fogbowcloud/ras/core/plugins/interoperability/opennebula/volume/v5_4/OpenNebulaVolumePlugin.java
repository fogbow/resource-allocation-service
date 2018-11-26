package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.util.Properties;

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
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

public class OpenNebulaVolumePlugin implements VolumePlugin<Token> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);
    
    public static final String DEFAULT_DATASTORE_ID = "default_datastore_id";
	public static final String BLOCK_DISK_TYPE = "BLOCK";
	public static final String DATABLOCK_IMAGE_TYPE = "DATABLOCK";
	public static final String FILE_SYSTEM_TYPE_RAW = "raw";
	public static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
	public static final String PERSISTENT_DISK_CONFIRMATION = "YES";
    
    private OpenNebulaClientFactory factory;
    
    private Integer dataStoreId;

	public OpenNebulaVolumePlugin() {
		String filePath = HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME;
		Properties properties = PropertiesUtil.readProperties(filePath);
		String dataStoreValue = properties.getProperty(DEFAULT_DATASTORE_ID);
		this.dataStoreId = dataStoreValue == null ? null : Integer.valueOf(dataStoreValue);
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(VolumeOrder volumeOrder, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		String volumeName = volumeOrder.getName();
		int volumeSize = volumeOrder.getVolumeSize();

		Client client = factory.createClient(localUserAttributes.getTokenValue());

		CreateVolumeRequest request = new CreateVolumeRequest.Builder()
				.name(volumeName)
				.size(volumeSize)
				.persistent(PERSISTENT_DISK_CONFIRMATION)
				.type(DATABLOCK_IMAGE_TYPE)
				.fileSystemType(FILE_SYSTEM_TYPE_RAW)
				.diskType(BLOCK_DISK_TYPE)
				.devicePrefix(DEFAULT_DATASTORE_DEVICE_PREFIX)
				.build();

		String template = request.getVolumeImage().marshalTemplate();
		return this.factory.allocateImage(client, template, this.dataStoreId);
	}

	@Override
	public VolumeInstance getInstance(String volumeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));

		int imageSize = Integer.parseInt(image.xpath(OpenNebulaTagNameConstants.SIZE));
		String imageName = image.getName();
		String imageState = image.stateString();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.VOLUME, imageState);

		return new VolumeInstance(volumeInstanceId, instanceState, imageName, imageSize);
	}

	@Override
	public void deleteInstance(String volumeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));
		OneResponse response = image.delete();
		if (response.isError()) {
			LOGGER.error(
					String.format(Messages.Error.ERROR_WHILE_REMOVING_VI, volumeInstanceId, response.getMessage()));
		}
	}

	protected void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;		
	}
}