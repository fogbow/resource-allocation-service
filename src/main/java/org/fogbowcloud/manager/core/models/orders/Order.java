package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.OrderState;
import org.fogbowcloud.manager.core.models.Token;

public class Order {

	private String id;
	private OrderState orderState;
	private Token localToken;
	private Token federationToken;
	private String requestingMember;
	private String providingMember;
	private String instanceId;
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

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public long getFulfilledTime() {
		return fulfilledTime;
	}

	public void setFulfilledTime(long fulfilledTime) {
		this.fulfilledTime = fulfilledTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((federationToken == null) ? 0 : federationToken.hashCode());
		result = prime * result + (int) (fulfilledTime ^ (fulfilledTime >>> 32));
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
		result = prime * result + ((localToken == null) ? 0 : localToken.hashCode());
		result = prime * result + ((orderState == null) ? 0 : orderState.hashCode());
		result = prime * result + ((providingMember == null) ? 0 : providingMember.hashCode());
		result = prime * result + ((requestingMember == null) ? 0 : requestingMember.hashCode());
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
		if (fulfilledTime != other.fulfilledTime)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (instanceId == null) {
			if (other.instanceId != null)
				return false;
		} else if (!instanceId.equals(other.instanceId))
			return false;
		if (localToken == null) {
			if (other.localToken != null)
				return false;
		} else if (!localToken.equals(other.localToken))
			return false;
		if (orderState == null) {
			if (other.orderState != null)
				return false;
		} else if (!orderState.equals(other.orderState))
			return false;
		if (providingMember == null) {
			if (other.providingMember != null)
				return false;
		} else if (!providingMember.equals(other.providingMember))
			return false;
		if (requestingMember == null) {
			if (other.requestingMember != null)
				return false;
		} else if (!requestingMember.equals(other.requestingMember))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Order [id=" + id + ", orderState=" + orderState + ", localToken=" + localToken + ", federationToken="
				+ federationToken + ", requestingMember=" + requestingMember + ", providingMember=" + providingMember
				+ ", instanceId=" + instanceId + ", fulfilledTime=" + fulfilledTime + "]";
	}
}
