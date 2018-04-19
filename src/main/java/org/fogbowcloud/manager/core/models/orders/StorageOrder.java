package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public class StorageOrder extends Order {

    private int storageSize;

	public StorageOrder(String id, OrderState orderState, Token localToken, Token federationToken,
						String requestingMember, String providingMember, OrderInstance orderInstance, long fulfilledTime) {
		super(id, orderState, localToken, federationToken, requestingMember, providingMember, orderInstance, fulfilledTime);
	}

	public int getStorageSize() {
		return storageSize;
	}

	public void setStorageSize(int storageSize) {
		this.storageSize = storageSize;
	}

}
