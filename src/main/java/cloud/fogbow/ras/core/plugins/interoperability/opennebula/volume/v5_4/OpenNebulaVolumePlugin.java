package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.io.File;
import java.util.Properties;

import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.common.util.cloud.opennebula.OpenNebulaTagNameConstants;

public class OpenNebulaVolumePlugin implements VolumePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);
    
    private static final String BLOCK_DISK_TYPE = "BLOCK";
    private static final String DATABLOCK_IMAGE_TYPE = "DATABLOCK";
    private static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
    private static final String DEFAULT_DATASTORE_ID = "default_datastore_id";
    private static final String FILE_SYSTEM_TYPE_RAW = "raw";
    private static final String PERSISTENT_DISK_CONFIRMATION = "YES";
    
	@Override
	public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudUser.getToken());

		String volumeName = volumeOrder.getName();
		int volumeSize = volumeOrder.getVolumeSize();
		
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
		Integer datastoreId = getDataStoreId();
		return OpenNebulaClientUtil.allocateImage(client, template, datastoreId);
	}

	@Override
	public VolumeInstance getInstance(String volumeInstanceId, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, volumeInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudUser.getToken());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));

		int imageSize = Integer.parseInt(image.xpath(OpenNebulaTagNameConstants.SIZE));
		String imageName = image.getName();
		String imageState = image.stateString();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.VOLUME, imageState);
		return new VolumeInstance(volumeInstanceId, instanceState, imageName, imageSize);
	}

	@Override
	public void deleteInstance(String volumeInstanceId, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, volumeInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudUser.getToken());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));
		OneResponse response = image.delete();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_VOLUME_IMAGE, volumeInstanceId, response.getMessage()));
		}
	}

	protected Integer getDataStoreId() {
		String dataStoreValue = getProperties().getProperty(DEFAULT_DATASTORE_ID);
		return dataStoreValue == null ? null : Integer.valueOf(dataStoreValue);
	}
	
	protected String getEndpoint() {
		Properties properties = getProperties();
		String endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		return endpoint;
	}
	
	protected Properties getProperties() {
		String opennebulaConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ SystemConstants.OPENNEBULA_CLOUD_NAME_DIRECTORY
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		
		Properties properties = PropertiesUtil.readProperties(opennebulaConfFilePath);
		return properties;
	}
	
}
