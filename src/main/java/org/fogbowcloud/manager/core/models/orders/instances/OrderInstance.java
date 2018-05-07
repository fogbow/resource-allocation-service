package org.fogbowcloud.manager.core.models.orders.instances;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "tb_order_instance")
public class OrderInstance {

	// TODO: the id should not be empty. Is necessary to check it in the
	// constructor method.
	@Id
	@Column(name = "id", nullable = false, unique = true)
	private String id;
	
    @Column(name="role")
    @NotNull(message = "State can not be null.")
	@Enumerated(EnumType.STRING)
	private InstanceState state;
    
    public OrderInstance() {}

	public OrderInstance(String id) {
		this.id = id;
	}

	public OrderInstance(String id, InstanceState state) {
		this(id);
		this.state = state;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public InstanceState getState() {
		return state;
	}

	public void setState(InstanceState state) {
		this.state = state;
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
		OrderInstance other = (OrderInstance) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
