package org.fogbowcloud.manager.core.models.orders;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "tb_order")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ComputeOrder.class, name = "COMPUTE"),
		@JsonSubTypes.Type(value = NetworkOrder.class, name = "NETWORK"),
		@JsonSubTypes.Type(value = StorageOrder.class, name = "STORAGE")})
public abstract class Order {

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
	private OrderInstance orderInstance;

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
		this.orderInstance = orderInstace;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((federationToken == null) ? 0 : federationToken.hashCode());
		result = prime * result + ((fulfilledTime == null) ? 0 : fulfilledTime.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((localToken == null) ? 0 : localToken.hashCode());
		result = prime * result + ((orderInstance == null) ? 0 : orderInstance.hashCode());
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
		if (orderInstance == null) {
			if (other.orderInstance != null)
				return false;
		} else if (!orderInstance.equals(other.orderInstance))
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
