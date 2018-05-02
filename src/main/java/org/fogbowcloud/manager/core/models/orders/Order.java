package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public abstract class Order {

	private String id;

	private OrderState orderState;

	private Token localToken;

	private Token federationToken;

	private String requestingMember;

	private String providingMember;

	private OrderInstance orderInstance;

	private Long fulfilledTime;

	/**
	 * Creating Order with predefined Id.
	 */
	public Order(String id, Token localToken, Token federationToken, String requestingMember, String providingMember) {
		this.id = id;
		this.orderState = OrderState.OPEN;
		this.localToken = localToken;
		this.federationToken = federationToken;
		this.requestingMember = requestingMember;
		this.providingMember = providingMember;
	}

	public String getId() {
		return id;
	}

	public synchronized OrderState getOrderState() {
		return orderState;
	}

	public synchronized void setOrderState(OrderState state) {
		this.orderState = state;
	}

	public Token getLocalToken() {
		return localToken;
	}

	public void setLocalToken(Token localToken) {
		this.localToken = localToken;
	}

	public Token getFederationToken() {
		return federationToken;
	}

	public void setFederationToken(Token federationToken) {
		this.federationToken = federationToken;
	}

	public String getRequestingMember() {
		return requestingMember;
	}

	public String getProvidingMember() {
		return providingMember;
	}

	public synchronized OrderInstance getOrderInstance() {
		return orderInstance;
	}

	public synchronized void setOrderInstance(OrderInstance orderInstance) {
		this.orderInstance = orderInstance;
	}

	public long getFulfilledTime() {
		return fulfilledTime;
	}

	public void setFulfilledTime(Long fulfilledTime) {
		this.fulfilledTime = fulfilledTime;
	}

	public boolean isLocal(String localMemberId) {
		return this.providingMember.equals(localMemberId);
	}

	public boolean isRemote(String localMemberId) {
		return !this.providingMember.equals(localMemberId);
	}

	public abstract OrderType getType();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Order other = (Order) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Order [id=" + id + ", orderState=" + orderState + ", localToken=" + localToken + ", federationToken="
				+ federationToken + ", requestingMember=" + requestingMember + ", providingMember=" + providingMember
				+ ", orderInstance=" + orderInstance + ", fulfilledTime=" + fulfilledTime + "]";
	}

}
