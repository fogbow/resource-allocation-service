package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public class StorageOrder extends Order {

    private int storageSize;

	public StorageOrder(OrderState orderState, Token localToken, Token federationToken, String requestingMember,
						String providingMember, OrderInstance orderInstace, long fulfilledTime, int storageSize) {
		super(orderState, localToken, federationToken, requestingMember, providingMember, orderInstace, fulfilledTime);
		this.storageSize = storageSize;
	}

	public int getStorageSize() {
		return storageSize;
	}

	public void setStorageSize(int storageSize) {
		this.storageSize = storageSize;
	}

	@Override
	public OrderType getType() {
		return OrderType.NETWORK;
	}

	@Override
	public void processOpenOrder(InstanceProvider instanceProvider) {
		super.processOpenOrder(instanceProvider);
	}

}
