package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

import javax.persistence.*;

@Entity
@DiscriminatorValue(value = "compute")
public class ComputeOrder extends Order {

	@Column(name = "vCPU")
	private int vCPU;

	/** Memory attribute, must be set in MB. */
	@Column(name = "memory")
	private int memory;

	/** Disk attribute, must be set in GB. */
	@Column(name = "disk")
	private int disk;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "user_data_id")
	private UserData userData;

	public ComputeOrder(){ }

	public ComputeOrder(OrderState orderState, Token localToken, Token federationToken, String requestingMember,
						String providingMember, OrderInstance orderInstance, long fulfilledTime, int vCPU, int memory,
						int disk, UserData userData) {
		super(orderState, localToken, federationToken, requestingMember, providingMember, orderInstance, fulfilledTime);
		this.vCPU = vCPU;
		this.memory = memory;
		this.disk = disk;
		this.userData = userData;
	}

	public int getvCPU() {
		return vCPU;
	}

	public void setvCPU(int vCPU) {
		this.vCPU = vCPU;
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
	}

	public int getDisk() {
		return disk;
	}

	public void setDisk(int disk) {
		this.disk = disk;
	}

	public UserData getUserData() {
		return userData;
	}

	public void setUserData(UserData userData) {
		this.userData = userData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + disk;
		result = prime * result + memory;
		result = prime * result + ((userData == null) ? 0 : userData.hashCode());
		result = prime * result + vCPU;
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
		ComputeOrder other = (ComputeOrder) obj;
		if (disk != other.disk)
			return false;
		if (memory != other.memory)
			return false;
		if (userData == null) {
			if (other.userData != null)
				return false;
		} else if (!userData.equals(other.userData))
			return false;
		if (vCPU != other.vCPU)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ComputeOrder [vCPU=" + vCPU + ", memory=" + memory + ", disk=" + disk + ", userData=" + userData + "]";
	}
}
