package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants;

public class OpenNebulaVolumePlugin implements VolumePlugin<CloudToken> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);
    
    private static final String BLOCK_DISK_TYPE = "BLOCK";
    private static final String CLOUD_NAME = "opennebula";
    private static final String DATABLOCK_IMAGE_TYPE = "DATABLOCK";
    private static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
    private static final String DEFAULT_DATASTORE_ID = "default_datastore_id";
    private static final String FILE_SYSTEM_TYPE_RAW = "raw";
    private static final String PERSISTENT_DISK_CONFIRMATION = "YES";
    
    private static final int CHMOD_PERMISSION_744 = 744;

    protected static final String OPENNEBULA_RPC_ENDPOINT_KEY = "opennebula_rpc_endpoint";

    private Integer dataStoreId;

	public OpenNebulaVolumePlugin() {
		String dataStoreValue = getProperties().getProperty(DEFAULT_DATASTORE_ID);
		this.dataStoreId = dataStoreValue == null ? null : Integer.valueOf(dataStoreValue);
	}

	@Override
	public String requestInstance(VolumeOrder volumeOrder, CloudToken cloudToken) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudToken.getTokenValue()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudToken.getTokenValue());

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
		return allocateImage(client, template, this.dataStoreId);
	}

	@Override
	public VolumeInstance getInstance(String volumeInstanceId, CloudToken cloudToken) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, volumeInstanceId, cloudToken.getTokenValue()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudToken.getTokenValue());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));

		int imageSize = Integer.parseInt(image.xpath(OpenNebulaTagNameConstants.SIZE));
		String imageName = image.getName();
		String imageState = image.stateString();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.VOLUME, imageState);
		return new VolumeInstance(volumeInstanceId, instanceState, imageName, imageSize);
	}

	@Override
	public void deleteInstance(String volumeInstanceId, CloudToken cloudToken) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, volumeInstanceId, cloudToken.getTokenValue()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudToken.getTokenValue());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(volumeInstanceId));
		OneResponse response = image.delete();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_VOLUME_IMAGE, volumeInstanceId, response.getMessage()));
		}
	}

	protected String allocateImage(Client client, String template, Integer datastoreId)
			throws InvalidParameterException {
		
		OneResponse response = Image.allocate(client, template, datastoreId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_IMAGE, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException(message);
		}
		Image.chmod(client, response.getIntMessage(), CHMOD_PERMISSION_744);
		return response.getMessage();
	}
	
	protected String getEndpoint() {
		Properties properties = getProperties();
		String endpoint = properties.getProperty(OPENNEBULA_RPC_ENDPOINT_KEY);
		return endpoint;
	}
	
	protected Properties getProperties() {
		String opennebulaConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		
		Properties properties = PropertiesUtil.readProperties(opennebulaConfFilePath);
		return properties;
	}
	
}
