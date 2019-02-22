package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.image.ImagePool;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.images.Image;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

public class OpenNebulaImagePlugin implements ImagePlugin<CloudToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaImagePlugin.class);
	
	private static final String CLOUD_NAME = "opennebula";
	private static final String IMAGE_SIZE_PATH = "IMAGE/SIZE";
	private static final String RESOURCE_NAME = "image";
	
	protected static final String OPENNEBULA_RPC_ENDPOINT_KEY = "opennebula_rpc_endpoint";

	@Override
	public Map<String, String> getAllImages(CloudToken cloudToken) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.RECEIVING_GET_ALL_REQUEST, RESOURCE_NAME));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudToken.getTokenValue());
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		
		Map<String, String> allImages = new HashMap<>();
		for (org.opennebula.client.image.Image image : imagePool) {
			allImages.put(image.getId(), image.getName());
		}
		return allImages;
	}

	@Override
	public Image getImage(String imageId, CloudToken cloudToken) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, imageId, cloudToken.getTokenValue()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudToken.getTokenValue());
		
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);		
		for (org.opennebula.client.image.Image image : imagePool) {
			if (image.getId().equals(imageId)) {
				return mountImage(image);
			}
		}
		LOGGER.info(String.format(Messages.Info.INSTANCE_NOT_FOUND, imageId));
		return null;
	}

	private Image mountImage(org.opennebula.client.image.Image image) {
		String id = image.getId();
		String name = image.getName();
		String imageSize = image.xpath(IMAGE_SIZE_PATH);
		
		int size = Integer.parseInt(imageSize);
		int minDisk = -1;
		int minRam = -1;
		int state = image.state();
		
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.IMAGE, state);
		String status = instanceState.getValue();

		return new Image(id, name, size, minDisk, minRam, status);
	}

	private String getEndpoint() {
		String opennebulaConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		
		Properties properties = PropertiesUtil.readProperties(opennebulaConfFilePath);
		String endpoint = properties.getProperty(OPENNEBULA_RPC_ENDPOINT_KEY);
		return endpoint;
	}

}
