package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.util.Properties;
import java.util.UUID;

import cloud.fogbow.ras.constants.SystemConstants;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import cloud.fogbow.common.constants.OpenNebulaConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
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
	private static final String IMAGE_TYPE = "IMAGE";
	private static final String DATASTORE_FREE_PATH_FORMAT = "//DATASTORE[%s]/FREE_MB";
    private static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
    private static final String FILE_SYSTEM_TYPE_RAW = "raw";
    private static final String PERSISTENT_DISK_CONFIRMATION = "YES";
	private static final int CONVERT_DISK = 1024;

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
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String volumeName = volumeOrder.getName();
		String name = volumeName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : volumeName;
		String imagePersistent = PERSISTENT_DISK_CONFIRMATION;
		String imageType = DATABLOCK_IMAGE_TYPE;
		String fileSystemType = FILE_SYSTEM_TYPE_RAW;
		String diskType = BLOCK_DISK_TYPE;
		String devicePrefix = DEFAULT_DATASTORE_DEVICE_PREFIX;
		int size = volumeOrder.getVolumeSize() * CONVERT_DISK;
		
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
		Integer datastoreId = getDataStoreId(client, size);
		return OpenNebulaClientUtil.allocateImage(client, template, datastoreId);
	}
	
	@Override
	public VolumeInstance getInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeOrder.getInstanceId()));

		int imageSize = Integer.parseInt(image.xpath(OpenNebulaConstants.SIZE)) / CONVERT_DISK;
		String imageName = image.getName();
		String imageState = image.stateString();
		return new VolumeInstance(volumeOrder.getInstanceId(), imageState, imageName, imageSize);
	}

	@Override
	public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeOrder.getInstanceId()));
		OneResponse response = image.delete();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_VOLUME_IMAGE, volumeOrder.getInstanceId(), response.getMessage()));
		}
	}

	protected Integer getDataStoreId(Client client, int diskSize) throws UnexpectedException {
		DatastorePool datastorePool = OpenNebulaClientUtil.getDatastorePool(client);

		int index = 1;
		for (Datastore datastore : datastorePool) {
		    Integer freeDiskSize = null;
			if (datastore.typeStr().equals(IMAGE_TYPE)) {
				try {
					freeDiskSize = Integer.valueOf(datastore.xpath(String.format(DATASTORE_FREE_PATH_FORMAT, index)));
				} catch(NumberFormatException e) {
					LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e.getMessage()));
					throw new UnexpectedException();
				}
			}

			if (freeDiskSize != null) {
			    if (freeDiskSize >= diskSize) return datastore.id();
			}

			index++;
		}

		return null;
	}
	
	protected String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

	// Used for testing only
	protected void setProperties(Properties properties) {
		this.properties = properties;
	}
}
