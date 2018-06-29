package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface VolumePlugin {

	public String requestInstance(VolumeOrder volumeOrder, Token localToken)
			throws FogbowManagerException;

	public VolumeInstance getInstance(String volumeInstanceId, Token localToken)
			throws FogbowManagerException;

	public void deleteInstance(String volumeInstanceId, Token localToken)
			throws FogbowManagerException;
	
}
