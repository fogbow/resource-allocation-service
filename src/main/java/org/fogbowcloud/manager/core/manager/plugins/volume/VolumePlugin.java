package org.fogbowcloud.manager.core.manager.plugins.volume;

import java.util.List;

import org.fogbowcloud.manager.core.models.orders.instances.VolumeOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface VolumePlugin {

	public String requestInstance(Token localToken);

	public List<VolumeOrderInstance> getInstances(Token token);

	public VolumeOrderInstance getInstance(Token token, String instanceId);

	public void removeInstance(Token token, String instanceId);

	public void removeInstances(Token token);
	
}
