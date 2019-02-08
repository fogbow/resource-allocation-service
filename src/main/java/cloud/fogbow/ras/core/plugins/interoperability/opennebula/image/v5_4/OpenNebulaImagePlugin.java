package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.util.HashMap;
import java.util.Map;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.images.Image;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.image.ImagePool;

public class OpenNebulaImagePlugin implements ImagePlugin {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaImagePlugin.class);
	private static final String IMAGE_SIZE_PATH = "IMAGE/SIZE";

	private OpenNebulaClientFactory factory;

	public OpenNebulaImagePlugin(String confFilePath) {
		this.factory = new OpenNebulaClientFactory(confFilePath);
	}

	@Override
	public Map<String, String> getAllImages(CloudToken localUserAttributes) throws FogbowException {
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);

		Map<String, String> allImages = new HashMap<>();
		for (org.opennebula.client.image.Image image : imagePool) {
			allImages.put(image.getId(), image.getName());
		}
		return allImages;
	}

	@Override
	public Image getImage(String imageId, CloudToken localUserAttributes) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, imageId, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);
		
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

	public void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;
	}

}
