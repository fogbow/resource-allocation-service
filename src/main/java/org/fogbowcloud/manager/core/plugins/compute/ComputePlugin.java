package org.fogbowcloud.manager.core.plugins.compute;

import java.util.List;

import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.StorageLink;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;

public interface ComputePlugin {

	/**
	 * This method requests the virtual machine creation on a provider.
	 *
	 * @param computeOrder {@link org.fogbowcloud.manager.core.models.orders.Order} for creating a virtual machine.
	 * @param imageId Instance image ID.
	 * @return Instance ID.
	 * @throws RequestException {@link RequestException} When request fails.
	 */
	public String requestInstance(ComputeOrder computeOrder, String imageId) throws RequestException;

	public ComputeOrderInstance getInstance(Token localToken, String instanceId) throws RequestException;

	public List<ComputeOrderInstance> getInstances(Token localToken);

	public void deleteInstance(Token localToken, OrderInstance instance) throws RequestException;

	public void deleteInstances(Token localToken);

	public String attachStorage(Token localToken, StorageLink storageLink);

	public String detachStorage(Token localToken, StorageLink storageLink);

	public String getImageId(Token localToken, String imageName);
}
