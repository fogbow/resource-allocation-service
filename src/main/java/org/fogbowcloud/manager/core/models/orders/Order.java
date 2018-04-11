package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

import javax.persistence.*;

@Entity
@Table(name = "order")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Column(name = "orderState")
	private OrderState orderState;

	@Column(name = "localToken")
	private Token localToken;

	@Column(name = "federationToken")
	private Token federationToken;

	@Column(name = "requestingMember")
	private String requestingMember;

	@Column(name = "providingMember")
	private String providingMember;

	@Column(name = "orderInstace")
	private OrderInstance orderInstace;

	@Column(name = "fulfilledTime")
	private long fulfilledTime;

	protected Order(){ }

	public Order(OrderState orderState, Token localToken, Token federationToken, String requestingMember,
				 String providingMember, OrderInstance orderInstace, long fulfilledTime) {
		this.orderState = orderState;
		this.localToken = localToken;
		this.federationToken = federationToken;
		this.requestingMember = requestingMember;
		this.providingMember = providingMember;
		this.orderInstace = orderInstace;
		this.fulfilledTime = fulfilledTime;
	}

	public Long getId() { return id; }

	public void setId(Long id) { this.id = id; }

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

	public OrderInstance getOrderInstace() {
		return orderInstace;
	}

	public void setOrderInstace(OrderInstance orderInstace) {
		this.orderInstace = orderInstace;
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
		result = prime * result + ((orderInstace == null) ? 0 : orderInstace.hashCode());
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
		if (orderInstace == null) {
			if (other.orderInstace != null)
				return false;
		} else if (!orderInstace.equals(other.orderInstace))
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
				+ ", orderInstace=" + orderInstace + ", fulfilledTime=" + fulfilledTime + "]";
	}
}
