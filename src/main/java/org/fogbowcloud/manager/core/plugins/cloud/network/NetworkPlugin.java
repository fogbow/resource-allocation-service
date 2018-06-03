package org.fogbowcloud.manager.core.plugins.cloud.network;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface NetworkPlugin {

	public String requestInstance(NetworkOrder networkOrder, Token localToken) throws RequestException;

	public NetworkInstance getInstance(String networkInstanceId, Token localToken) throws RequestException;

	public void deleteInstance(String networkInstanceId, Token localToken) throws RequestException;
	
}
