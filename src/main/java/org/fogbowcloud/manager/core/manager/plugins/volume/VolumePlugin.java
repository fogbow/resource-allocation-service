package org.fogbowcloud.manager.core.manager.plugins.volume;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.instances.VolumeOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

import java.util.List;

public interface VolumePlugin {

	public String requestInstance(Token localToken, VolumeOrderInstance storageOrderInstance)
			throws RequestException;

	public VolumeOrderInstance getInstance(Token token, String storageOrderInstanceId)
			throws RequestException;

	public void removeInstance(Token token, String storageOrderInstanceId) 
			throws RequestException;
	
}
