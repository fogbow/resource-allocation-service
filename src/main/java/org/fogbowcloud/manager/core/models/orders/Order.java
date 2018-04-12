package org.fogbowcloud.manager.core.models.orders;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

@Entity
@Table(name = "tb_order")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

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
	private OrderInstance orderInstace;

	@Column(name = "fulfilledTime")
	private Long fulfilledTime;

	public Order() {
	
	}

	public Order(OrderState orderState, Token localToken, Token federationToken, String requestingMember,
			String providingMember, OrderInstance orderInstace, Long fulfilledTime) {
		this.orderState = orderState;
		this.localToken = localToken;
		this.federationToken = federationToken;
		this.requestingMember = requestingMember;
		this.providingMember = providingMember;
		this.orderInstace = orderInstace;
		this.fulfilledTime = fulfilledTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	 public OrderInstance getOrderInstace() {
	 return orderInstace;
	 }
	
	 public void setOrderInstace(OrderInstance orderInstace) {
	 this.orderInstace = orderInstace;
	 }

	public long getFulfilledTime() {
		return fulfilledTime;
	}

	public void setFulfilledTime(Long fulfilledTime) {
		this.fulfilledTime = fulfilledTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((federationToken == null) ? 0 : federationToken.hashCode());
		result = prime * result + ((fulfilledTime == null) ? 0 : fulfilledTime.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((localToken == null) ? 0 : localToken.hashCode());
		result = prime * result + ((orderInstace == null) ? 0 : orderInstace.hashCode());
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
		if (fulfilledTime == null) {
			if (other.fulfilledTime != null)
				return false;
		} else if (!fulfilledTime.equals(other.fulfilledTime))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (localToken == null) {
			if (other.localToken != null)
				return false;
		} else if (!localToken.equals(other.localToken))
			return false;
		if (orderInstace == null) {
			if (other.orderInstace != null)
				return false;
		} else if (!orderInstace.equals(other.orderInstace))
			return false;
		if (orderState != other.orderState)
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
}
