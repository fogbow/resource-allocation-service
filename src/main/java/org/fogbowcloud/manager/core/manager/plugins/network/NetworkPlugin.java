package org.fogbowcloud.manager.core.manager.plugins.network;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.instances.NetworkOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface NetworkPlugin {

	public String requestInstance(NetworkOrder order, Token localToken) throws RequestException;

	public NetworkOrderInstance getInstance(Token token, String instanceId) throws RequestException;

	public void deleteInstance(Token token, String instanceId) throws RequestException;
	
}
