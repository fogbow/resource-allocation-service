package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.token.Token;

public class StorageOrder extends Order {

	private int storageSize;

	/**
	 * Creating Order with predefined Id.
	 */
	public StorageOrder(String id, Token federationToken, String requestingMember,
			String providingMember, int storageSize) {
		super(id, federationToken, requestingMember, providingMember);
		this.storageSize = storageSize;
	}

	public StorageOrder(Token federationToken, String requestingMember, String providingMember,
			int storageSize) {
		this(UUID.randomUUID().toString(), federationToken, requestingMember, providingMember, storageSize);
	}

	public int getStorageSize() {
		return storageSize;
	}

	public void setStorageSize(int storageSize) {
		this.storageSize = storageSize;
	}

	@Override
	public OrderType getType() {
		return OrderType.STORAGE;
	}
}
