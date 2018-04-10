package org.fogbowcloud.manager.core.plugins;

import java.util.List;

import org.fogbowcloud.manager.core.models.ComputeInstance;
import org.fogbowcloud.manager.core.models.StorageLink;
import org.fogbowcloud.manager.core.models.Token;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;

public interface ComputePlugin {

	public String requestInstance(ComputeOrder computeOrder, String imageId);

	public ComputeInstance getInstance(Token localToken, String instanceId);

	public List<ComputeInstance> getInstances(Token localToken);

	public void removeInstance(Token localToken, String instanceId);

	public void removeInstances(Token localToken);

	public String attachStorage(Token localToken, StorageLink storageLink);

	public String detachStorage(Token localToken, StorageLink storageLink);

	public String getImageId(Token localToken, String imageName);
}
