package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface VolumePlugin {

	public String requestInstance(VolumeOrder volumeOrder, Token localToken)
			throws RequestException;

	public VolumeInstance getInstance(String volumeInstanceId, Token localToken)
			throws RequestException;

	public void deleteInstance(String volumeInstanceId, Token localToken)
			throws RequestException;
	
}
