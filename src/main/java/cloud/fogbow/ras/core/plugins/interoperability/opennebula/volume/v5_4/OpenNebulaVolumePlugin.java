package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import java.util.Properties;

public class OpenNebulaVolumePlugin implements VolumePlugin {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);
    
    public static final String DEFAULT_DATASTORE_ID = "default_datastore_id";
	public static final String BLOCK_DISK_TYPE = "BLOCK";
	public static final String DATABLOCK_IMAGE_TYPE = "DATABLOCK";
	public static final String FILE_SYSTEM_TYPE_RAW = "raw";
	public static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
	public static final String PERSISTENT_DISK_CONFIRMATION = "YES";
    
    private OpenNebulaClientFactory factory;
    
    private Integer dataStoreId;

	public OpenNebulaVolumePlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		String dataStoreValue = properties.getProperty(DEFAULT_DATASTORE_ID);
		this.dataStoreId = dataStoreValue == null ? null : Integer.valueOf(dataStoreValue);
		this.factory = new OpenNebulaClientFactory(confFilePath);
	}

	@Override
	public String requestInstance(VolumeOrder volumeOrder, CloudToken localUserAttributes) throws FogbowException {
		
		String volumeName = volumeOrder.getName();
		int volumeSize = volumeOrder.getVolumeSize();

		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

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
	public VolumeInstance getInstance(String volumeInstanceId, CloudToken localUserAttributes) throws FogbowException {
		
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
	public void deleteInstance(String volumeInstanceId, CloudToken localUserAttributes) throws FogbowException {

		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));
		OneResponse response = image.delete();
		if (response.isError()) {
			LOGGER.error(
					String.format(Messages.Error.ERROR_WHILE_REMOVING_VOLUME_IMAGE, volumeInstanceId, response.getMessage()));
		}
	}

	protected void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;		
	}
}
