package org.fogbowcloud.manager.core.models.orders;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "tb_order")
@JsonSubTypes({ @JsonSubTypes.Type(value = ComputeOrder.class, name = "COMPUTE"),
		@JsonSubTypes.Type(value = NetworkOrder.class, name = "NETWORK"),
		@JsonSubTypes.Type(value = StorageOrder.class, name = "STORAGE")})
public abstract class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false, unique = true)
	private String id;

	@Column(name = "order_state")
	@NotNull(message = "Order state can not be null.")
	@Enumerated(EnumType.STRING)
	private OrderState orderState;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "local_token_id")
	private Token localToken;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "fed_token_id")
	private Token federationToken;

	@Column(name = "requestingMember")
	private String requestingMember;

	@Column(name = "providingMember")
	private String providingMember;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "order_instance")
	private OrderInstance orderInstance;

	@Column(name = "fulfilledTime")
	private Long fulfilledTime;
	
	
	public Order() {}
	
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

	public void setId(String id) {
		this.id = id;
	}

	public void setFederationToken(Token federationToken) {
		this.federationToken = federationToken;
	}

	public void setRequestingMember(String requestingMember) {
		this.requestingMember = requestingMember;
	}

	public void setProvidingMember(String providingMember) {
		this.providingMember = providingMember;
	}

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
				+ ", orderInstace=" + orderInstance + ", fulfilledTime=" + fulfilledTime + "]";
	}

}
