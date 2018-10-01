package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ImagePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.ImagePool;

public class OpenNebulaImagePlugin implements ImagePlugin<Token> {

	private final static Logger LOGGER = Logger.getLogger(OpenNebulaImagePlugin.class);
	
	private OpenNebulaClientFactory factory;
	
	@Override
	public Map<String, String> getAllImages(Token localUserAttributes) throws FogbowRasException, UnexpectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Image getImage(String imageId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		ImagePool imagePool = this.factory.createImagePool(client);
		OneResponse response = imagePool.info();
		
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, response.getErrorMessage()));
			throw new UnexpectedException(response.getErrorMessage());
		}
		
		// FIXME
		for (org.opennebula.client.image.Image image : imagePool) {
			if (image.getId().equals(imageId)) {
				return null; 
			}
		}
		return null;
	}

}
