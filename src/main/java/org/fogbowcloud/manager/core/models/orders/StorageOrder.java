package org.fogbowcloud.manager.core.models.orders;

public class StorageOrder extends Order {

    private int storageSize;

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
	public void handleOpenOrder() {
		// TODO Auto-generated method stub
		
	}

}
