package org.fogbowcloud.manager.core.manager.plugins.volume;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface VolumePlugin {

	public String requestInstance(VolumeOrder order, Token localToken)
			throws RequestException;

	public VolumeInstance getInstance(Token token, String storageOrderInstanceId)
			throws RequestException;

	public void deleteInstance(Token token, String storageOrderInstanceId)
			throws RequestException;
	
}
