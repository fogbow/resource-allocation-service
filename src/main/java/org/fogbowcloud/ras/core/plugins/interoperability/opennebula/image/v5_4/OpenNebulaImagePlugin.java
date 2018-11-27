package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ImagePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.opennebula.client.Client;
import org.opennebula.client.image.ImagePool;

public class OpenNebulaImagePlugin implements ImagePlugin<Token> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaImagePlugin.class);
	private static final String IMAGE_SIZE_PATH = "IMAGE/SIZE";

	private OpenNebulaClientFactory factory;

	@Override
	public Map<String, String> getAllImages(Token localUserAttributes) throws FogbowRasException, UnexpectedException {
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);

		Map<String, String> allImages = new HashMap<>();
		for (org.opennebula.client.image.Image image : imagePool) {
			allImages.put(image.getId(), image.getName());
		}
		return allImages;
	}

	@Override
	public Image getImage(String imageId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, imageId, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);
		
		for (org.opennebula.client.image.Image image : imagePool) {
			if (image.getId().equals(imageId)) {
				return mountImage(image);
			}
		}
		LOGGER.info(String.format(Messages.Info.INSTANCE_WAS_NOT_FOUND, imageId));
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
