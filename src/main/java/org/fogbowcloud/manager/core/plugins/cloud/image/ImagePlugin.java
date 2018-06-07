package org.fogbowcloud.manager.core.plugins.cloud.image;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.ImageException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.token.Token;

public interface ImagePlugin {
	
	
	public Map<String, String> getImages(Token localToken) throws ImageException;
	
	public Image getImage(Token localToken, String id);
	
}
