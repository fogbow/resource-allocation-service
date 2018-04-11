package org.fogbowcloud.manager.core.plugins.compute.openstack;

import java.util.List;

import org.fogbowcloud.manager.core.models.StorageLink;
import org.fogbowcloud.manager.core.models.Token;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin {
	
	public OpenStackNovaV2ComputePlugin() {
		
	}

	public String requestInstance(ComputeOrder computeOrder, String imageId) {
		// TODO Auto-generated method stub
		return null;
	}

	public ComputeOrderInstance getInstance(Token localToken, String instanceId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ComputeOrderInstance> getInstances(Token localToken) {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeInstance(Token localToken, String instanceId) {
		// TODO Auto-generated method stub
	}

	public void removeInstances(Token localToken) {
		// TODO Auto-generated method stub

	}

	public String attachStorage(Token localToken, StorageLink storageLink) {
		// TODO Auto-generated method stub
		return null;
	}

	public String detachStorage(Token localToken, StorageLink storageLink) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getImageId(Token localToken, String imageName) {
		// TODO Auto-generated method stub
		return null;
	}
}
