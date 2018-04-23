package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.token.Token;

public class StorageOrder extends Order {

	private int storageSize;

	/**
	 * Creating Order with predefined Id.
	 */
	public StorageOrder(String id, Token localToken, Token federationToken, String requestingMember, String providingMember,
			int storageSize) {
		super(id, localToken, federationToken, requestingMember, providingMember);
		this.storageSize = storageSize;
	}
	
	public StorageOrder(Token localToken, Token federationToken, String requestingMember, String providingMember,
			int storageSize) {
		super(localToken, federationToken, requestingMember, providingMember);
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

	/**
	 * These method handle and request an open order, for this, processOpenOrder
	 * handle the Order to be ready to change your state and request the
	 * Instance from the InstanceProvider.
	 */
	@Override
	public synchronized void processOpenOrder(InstanceProvider instanceProvider) {
		super.processOpenOrder(instanceProvider);
	}

}
