package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;

public interface NetworkPlugin<T extends LocalUserAttributes> {

	public String requestInstance(NetworkOrder networkOrder, T localUserAttributes) throws FogbowManagerException, UnexpectedException;

	public NetworkInstance getInstance(String networkInstanceId, T localUserAttributes) throws FogbowManagerException, UnexpectedException;

	public void deleteInstance(String networkInstanceId, T localUserAttributes) throws FogbowManagerException, UnexpectedException;
	
}
