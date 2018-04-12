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
	private long fulfilledTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public OrderState getOrderState() {
		return orderState;
	}

	public void setOrderState(OrderState orderState) {
		this.orderState = orderState;
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

	public void setRequestingMember(String requestingMember) {
		this.requestingMember = requestingMember;
	}

	public String getProvidingMember() {
		return providingMember;
	}

	public void setProvidingMember(String providingMember) {
		this.providingMember = providingMember;
	}

	public OrderInstance getOrderInstance() {
		return orderInstance;
	}

	public void setOrderInstance(OrderInstance orderInstace) {
		this.orderInstance = orderInstace;
	}

	public long getFulfilledTime() {
		return fulfilledTime;
	}

	public void setFulfilledTime(long fulfilledTime) {
		this.fulfilledTime = fulfilledTime;
	}
	
	public boolean isLocal() {
		return this.providingMember.equals(this.requestingMember);
	}
	
	public boolean isRemote() {
		return !this.providingMember.equals(this.requestingMember);
	}

	public abstract void handleOpenOrder();
	
	public abstract OrderType getType();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((federationToken == null) ? 0 : federationToken.hashCode());
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
		if (federationToken == null) {
			if (other.federationToken != null)
				return false;
		} else if (!federationToken.equals(other.federationToken))
			return false;
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
				+ ", orderInstace=" + orderInstance + ", fulfilledTime=" + fulfilledTime + "]";
	}
}
