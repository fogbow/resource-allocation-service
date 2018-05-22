package org.fogbowcloud.manager.core.manager.plugins.network;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.instances.NetworkOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface NetworkPlugin {

	public String requestInstance(Token localToken);

	public NetworkOrderInstance getInstance(Token token, String instanceId);

	public void deleteInstance(Token token, String instanceId) throws RequestException;
	
}
