package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.token.Token;

public class NetworkOrder extends Order {

	private String gateway;
	private String address;
	private String allocation;

	/**
	 * Creating Order with predefined Id.
	 */
	public NetworkOrder(String id, Token localToken, Token federationToken, String requestingMember,
			String providingMember, String gateway, String address, String allocation) {
		super(id, localToken, federationToken, requestingMember, providingMember);
		this.gateway = gateway;
		this.address = address;
		this.allocation = allocation;
	}

	public NetworkOrder(Token localToken, Token federationToken, String requestingMember, String providingMember,
			String gateway, String address, String allocation) {
		this(UUID.randomUUID().toString(), localToken, federationToken, requestingMember, providingMember, gateway,
				address, allocation);
	}

	public String getGateway() {
		return gateway;
	}

	public String getAddress() {
		return address;
	}

	public String getAllocation() {
		return allocation;
	}

	@Override
	public OrderType getType() {
		return OrderType.NETWORK;
	}

	/**
	 * These method handle an open order, for this, handleOpenOrder handle the
	 * Order to be ready to change your state from OPEN to SPAWNING.
	 */
	@Override
	public synchronized void handleOpenOrder() {
	}

}
