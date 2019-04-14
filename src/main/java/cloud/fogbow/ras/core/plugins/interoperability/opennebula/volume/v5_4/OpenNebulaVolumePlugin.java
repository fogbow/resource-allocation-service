package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaTagNameConstants;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

public class OpenNebulaVolumePlugin implements VolumePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);
    
    private static final String BLOCK_DISK_TYPE = "BLOCK";
    private static final String DATABLOCK_IMAGE_TYPE = "DATABLOCK";
    private static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
    private static final String FILE_SYSTEM_TYPE_RAW = "raw";
    private static final String FOGBOW_VOLUME_NAME = "ras-volume-";
    private static final String PERSISTENT_DISK_CONFIRMATION = "YES";
    
    protected static final String DEFAULT_DATASTORE_ID = "default_datastore_id";
    
    private Properties properties;
    private String endpoint;

	public OpenNebulaVolumePlugin(String confFilePath) throws FatalErrorException {
		this.properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = this.properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
	}

	@Override
	public boolean isReady(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String volumeName = volumeOrder.getName();
		String name = volumeName == null ? FOGBOW_VOLUME_NAME + getRandomUUID() : volumeName;
		String imagePersistent = PERSISTENT_DISK_CONFIRMATION;
		String imageType = DATABLOCK_IMAGE_TYPE;
		String fileSystemType = FILE_SYSTEM_TYPE_RAW;
		String diskType = BLOCK_DISK_TYPE;
		String devicePrefix = DEFAULT_DATASTORE_DEVICE_PREFIX;
		int size = volumeOrder.getVolumeSize();
		
		CreateVolumeRequest request = new CreateVolumeRequest.Builder()
				.name(name)
				.size(size)
				.imagePersistent(imagePersistent)
				.imageType(imageType)
				.fileSystemType(fileSystemType)
				.diskType(diskType)
				.devicePrefix(devicePrefix)
				.build();

		String template = request.getVolumeImage().marshalTemplate();
		Integer datastoreId = getDataStoreId();
		return OpenNebulaClientUtil.allocateImage(client, template, datastoreId);
	}
	
	@Override
	public VolumeInstance getInstance(String volumeInstanceId, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, volumeInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));

		int imageSize = Integer.parseInt(image.xpath(OpenNebulaTagNameConstants.SIZE));
		String imageName = image.getName();
		String imageState = image.stateString();
		return new VolumeInstance(volumeInstanceId, imageState, imageName, imageSize);
	}

	@Override
	public void deleteInstance(String volumeInstanceId, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, volumeInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));
		OneResponse response = image.delete();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_VOLUME_IMAGE, volumeInstanceId, response.getMessage()));
		}
	}

	protected Integer getDataStoreId() throws UnexpectedException {
		String dataStore = this.properties.getProperty(DEFAULT_DATASTORE_ID);
		try {
			return Integer.valueOf(dataStore);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e.getMessage()));
			throw new UnexpectedException();
		}
	}
	
	protected String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

	// Used for testing only
	protected void setProperties(Properties properties) {
		this.properties = properties;
	}
}
