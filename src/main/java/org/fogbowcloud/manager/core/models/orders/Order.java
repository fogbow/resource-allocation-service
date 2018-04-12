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

	public void setOrderInstance(OrderInstance orderInstace) {
		this.orderInstance = orderInstace;
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
			return false;
		if (orderState != other.orderState)
		return true;
	}

	@Override
	public String toString() {
		return "Order [id=" + id + ", orderState=" + orderState + ", localToken=" + localToken + ", federationToken="
				+ federationToken + ", requestingMember=" + requestingMember + ", providingMember=" + providingMember
				+ ", orderInstace=" + orderInstance + ", fulfilledTime=" + fulfilledTime + "]";
	}
}
