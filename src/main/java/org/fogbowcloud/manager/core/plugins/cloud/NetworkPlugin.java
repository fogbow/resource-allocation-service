package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.Token;

public interface NetworkPlugin {

	public String requestInstance(NetworkOrder networkOrder, Token localToken) throws FogbowManagerException, UnexpectedException;

	public NetworkInstance getInstance(String networkInstanceId, Token localToken) throws FogbowManagerException, UnexpectedException;

	public void deleteInstance(String networkInstanceId, Token localToken) throws FogbowManagerException, UnexpectedException;
	
}
