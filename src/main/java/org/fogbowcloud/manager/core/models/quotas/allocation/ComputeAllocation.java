package org.fogbowcloud.manager.core.models.quotas.allocation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ComputeAllocation extends Allocation {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column
	private long id;
	
	@Column
	private int vCPU;
	
	@Column
	private int ram;
	
	@Column
	private int instances;
	
	public ComputeAllocation(int vCPU, int ram, int instances) {
		this.vCPU = vCPU;
		this.ram = ram;
		this.instances = instances;
	}
	
	public int getvCPU() {
		return this.vCPU;
	}

	public int getRam() {
		return this.ram;
	}

	public int getInstances() {
		return this.instances;
	}
}
