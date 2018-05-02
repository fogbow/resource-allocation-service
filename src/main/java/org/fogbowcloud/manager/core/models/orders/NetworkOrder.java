package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.token.Token;

public class NetworkOrder extends Order {

	private String gateway;
	private String address;
	private String allocation;

	/**
	 * Creating Order with predefined Id.
	 */
	public NetworkOrder(String id, Token localToken, Token federationToken, String requestingMember, String providingMember,
			String gateway, String address, String allocation) {
		super(id, localToken, federationToken, requestingMember, providingMember);
		this.gateway = gateway;
		this.address = address;
		this.allocation = allocation;
	}
	
	public NetworkOrder(Token localToken, Token federationToken, String requestingMember, String providingMember,
			String gateway, String address, String allocation) {
		super(localToken, federationToken, requestingMember, providingMember);
		this.gateway = gateway;
		this.address = address;
		this.allocation = allocation;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAllocation() {
		return allocation;
	}

	public void setAllocation(String allocation) {
		this.allocation = allocation;
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
