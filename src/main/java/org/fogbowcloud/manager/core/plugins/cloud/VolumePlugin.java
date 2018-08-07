package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;

public interface VolumePlugin<T extends LocalUserAttributes> {

	public String requestInstance(VolumeOrder volumeOrder, T localUserAttributes)
            throws FogbowManagerException, UnexpectedException;

	public VolumeInstance getInstance(String volumeInstanceId, T localUserAttributes)
			throws FogbowManagerException, UnexpectedException;

	public void deleteInstance(String volumeInstanceId, T localUserAttributes)
            throws FogbowManagerException, UnexpectedException;
	
}
