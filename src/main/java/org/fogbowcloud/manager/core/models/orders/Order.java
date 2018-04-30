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

	public Order() {
	
	}

	public Order(OrderState orderState, Token localToken, Token federationToken, String requestingMember,
				 String providingMember, OrderInstance orderInstance, Long fulfilledTime) {
		this.orderState = orderState;
		this.localToken = localToken;
		this.federationToken = federationToken;
		this.requestingMember = requestingMember;
		this.providingMember = providingMember;
		this.orderInstance = orderInstance;
		this.fulfilledTime = fulfilledTime;
	}

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


	public void setOrderInstance(OrderInstance orderInstance) {
		this.orderInstance = orderInstance;
	}

	public long getFulfilledTime() {
		return fulfilledTime;
	}

	public void setFulfilledTime(Long fulfilledTime) {
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Order order = (Order) o;

		if (id != null ? !id.equals(order.id) : order.id != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "Order{" +
				"id='" + id + '\'' +
				", orderState=" + orderState +
				", localToken=" + localToken +
				", federationToken=" + federationToken +
				", requestingMember='" + requestingMember + '\'' +
				", providingMember='" + providingMember + '\'' +
				", orderInstance=" + orderInstance +
				", fulfilledTime=" + fulfilledTime +
				'}';
	}
}
