package org.fogbowcloud.manager.core.plugins.compute;

import java.util.List;

import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.StorageLink;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;

public interface ComputePlugin {

	public String requestInstance(ComputeOrder computeOrder, String imageId) throws RequestException;

	public ComputeOrderInstance getInstance(Token localToken, String instanceId) throws RequestException;

	public List<ComputeOrderInstance> getInstances(Token localToken) throws RequestException;

	public void removeInstance(Token localToken, String instanceId);

	public void removeInstances(Token localToken);

	public String attachStorage(Token localToken, StorageLink storageLink);

	public String detachStorage(Token localToken, StorageLink storageLink);

	public String getImageId(Token localToken, String imageName);
}
