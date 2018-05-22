package org.fogbowcloud.manager.core.manager.plugins.volume;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.instances.StorageOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface VolumePlugin {

	public String requestInstance(Token localToken, StorageOrderInstance storageOrderInstance) 
			throws RequestException;

	public StorageOrderInstance getInstance(Token token, String storageOrderInstanceId) 
			throws RequestException;

	public void removeInstance(Token token, String storageOrderInstanceId) 
			throws RequestException;
	
}
